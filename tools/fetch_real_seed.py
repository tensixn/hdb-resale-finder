"""Builds flats.csv and transactions.csv from real data.gov.sg resale records, geocoded with OneMap.

No API keys needed. towns.csv and schools.csv are left alone.
Run: python tools/fetch_real_seed.py --months 12 --per-town 60
"""
import argparse
import csv
import json
import random
import time
import urllib.parse
import urllib.request
from datetime import date
from pathlib import Path

SEED = Path(__file__).resolve().parent.parent / "backend" / "src" / "main" / "resources" / "seed"
RESALE = "https://data.gov.sg/api/action/datastore_search"
RESOURCE = "d_8b84c4ee58e3cfc0ece0d773c8ca6abc"
ONEMAP = "https://www.onemap.gov.sg/api/common/elastic/search"
UA = {"User-Agent": "hdb-resale-finder-seed/1.0"}


def get_json(url, tries=6):
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code != 429 and e.code < 500 or i == tries - 1:
                raise
        except urllib.error.URLError:
            if i == tries - 1:
                raise
        time.sleep(2 ** (i + 1))


def last_months(n):
    y, m = date.today().year, date.today().month
    out = []
    for _ in range(n):
        out.append(f"{y:04d}-{m:02d}")
        y, m = (y - 1, 12) if m == 1 else (y, m - 1)
    return out


def fetch_month(month):
    rows, offset = [], 0
    while True:
        q = urllib.parse.urlencode({"resource_id": RESOURCE, "limit": 5000, "offset": offset,
                                    "filters": json.dumps({"month": month})})
        res = get_json(f"{RESALE}?{q}")["result"]
        rows += res["records"]
        offset += 5000
        if offset >= res.get("total", 0) or not res["records"]:
            return rows


def geocode(block, street):
    q = urllib.parse.urlencode({"searchVal": f"{block} {street}", "returnGeom": "Y",
                                "getAddrDetails": "Y", "pageNum": 1})
    for r in get_json(f"{ONEMAP}?{q}").get("results", []):
        if r.get("BLK_NO", "").upper() == block.upper():
            return float(r["LATITUDE"]), float(r["LONGITUDE"])
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--months", type=int, default=12)
    ap.add_argument("--per-town", type=int, default=60)
    a = ap.parse_args()

    with open(SEED / "towns.csv", newline="") as f:
        region = {r["town"]: r["region"] for r in csv.DictReader(f)}

    # Same identity as the flat table's unique key (block, street, flat_type, storey_range).
    key = ("block", "street_name", "town", "flat_type", "storey_range")
    sales = {}  # key -> list of (month, price, area, lease)
    for month in last_months(a.months):
        rows = fetch_month(month)
        print(f"{month}: {len(rows)} records")
        for r in rows:
            if r["town"] in region:
                sales.setdefault(tuple(r[c] for c in key), []).append(
                    (r["month"], round(float(r["resale_price"])), r["floor_area_sqm"], r["lease_commence_date"]))
    # Different units (area/lease) can share that key: keep only the most recent unit's sales.
    flats, attrs = {}, {}
    for k, s in sales.items():
        _, _, area, lease = max(s)
        attrs[k] = (area, lease)
        flats[k] = [(m, p) for m, p, ar, le in s if (ar, le) == (area, lease)]

    rng = random.Random(2006)
    by_town = {}
    for k in sorted(flats):
        by_town.setdefault(k[2], []).append(k)

    this_year = date.today().year
    flat_rows, tx_rows, missing, fid = [], [], 0, 0
    for town in sorted(by_town):
        picks = by_town[town]
        rng.shuffle(picks)
        kept = 0
        for k in picks:
            if kept >= a.per_town:
                break
            block, street, _, ftype, storey = k
            area, lease = attrs[k]
            time.sleep(0.3)
            pos = geocode(block, street)
            if not pos:
                missing += 1
                print(f"  not found: {block} {street}")
                continue
            fid += 1
            kept += 1
            lease_y = int(lease)
            flat_rows.append([fid, block, street, town, ftype, float(area), storey, lease_y,
                              99 - (this_year - lease_y), round(pos[0], 6), round(pos[1], 6), region[town]])
            # set(): resale_transaction is unique on (flat, month, price)
            tx_rows += [[fid, m, p] for m, p in sorted(set(flats[k]), reverse=True)]
        print(f"{town}: {kept} flats")

    with open(SEED / "flats.csv", "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["id", "block", "street", "town", "flat_type", "floor_area_sqm", "storey_range",
                    "lease_commence_year", "remaining_lease_years", "latitude", "longitude", "region"])
        w.writerows(flat_rows)
    with open(SEED / "transactions.csv", "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["flat_id", "month", "resale_price"])
        w.writerows(tx_rows)
    print(f"wrote {len(flat_rows)} flats, {len(tx_rows)} transactions, {missing} blocks not found")


if __name__ == "__main__":
    main()
