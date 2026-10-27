"""Generates the sample data the backend loads on first start.

The real sources (data.gov.sg resale prices, SPF crime figures, URA/SingStat density, MOE schools)
either need network access or are not available as an API, so the app ships with a deterministic
sample built from approximate public figures. Re-run with: python3 tools/generate_seed.py
"""
import csv
import math
import random
from datetime import date
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "backend" / "src" / "main" / "resources" / "seed"
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(2006)

# town, lat, lng, region, crime per 1,000 residents, residents per hectare, base $ psm, lease start range
TOWNS = [
    ("ANG MO KIO", 1.3691, 103.8454, "north", 4.1, 290, 6300, (1978, 2018)),
    ("BEDOK", 1.3236, 103.9273, "east", 4.4, 270, 5800, (1977, 2016)),
    ("BISHAN", 1.3508, 103.8485, "central", 3.2, 230, 7000, (1986, 2012)),
    ("BUKIT BATOK", 1.3590, 103.7637, "west", 3.6, 240, 5600, (1982, 2020)),
    ("BUKIT MERAH", 1.2819, 103.8239, "south", 5.3, 320, 7500, (1974, 2019)),
    ("BUKIT PANJANG", 1.3774, 103.7719, "west", 3.1, 220, 5300, (1987, 2019)),
    ("CENTRAL AREA", 1.2903, 103.8519, "south", 7.8, 380, 8400, (1972, 2011)),
    ("CHOA CHU KANG", 1.3840, 103.7470, "west", 3.0, 210, 5000, (1988, 2019)),
    ("CLEMENTI", 1.3162, 103.7649, "west", 3.5, 260, 7000, (1979, 2019)),
    ("GEYLANG", 1.3201, 103.8918, "east", 6.9, 310, 6200, (1976, 2015)),
    ("HOUGANG", 1.3612, 103.8863, "north", 3.4, 250, 5500, (1984, 2018)),
    ("JURONG EAST", 1.3329, 103.7436, "west", 4.0, 230, 5700, (1983, 2016)),
    ("JURONG WEST", 1.3404, 103.7090, "west", 3.7, 260, 5000, (1985, 2020)),
    ("KALLANG/WHAMPOA", 1.3100, 103.8651, "central", 5.6, 330, 7000, (1975, 2017)),
    ("MARINE PARADE", 1.3040, 103.9072, "east", 3.9, 280, 6800, (1975, 2003)),
    ("PASIR RIS", 1.3721, 103.9474, "east", 2.8, 180, 5300, (1989, 2018)),
    ("PUNGGOL", 1.4043, 103.9020, "north", 2.6, 240, 6100, (2003, 2021)),
    ("QUEENSTOWN", 1.2942, 103.7861, "south", 4.6, 300, 7800, (1970, 2020)),
    ("SEMBAWANG", 1.4491, 103.8185, "north", 2.9, 170, 4900, (1998, 2020)),
    ("SENGKANG", 1.3868, 103.8914, "north", 2.7, 250, 5900, (1999, 2021)),
    ("SERANGOON", 1.3554, 103.8679, "north", 3.3, 210, 6000, (1984, 2015)),
    ("TAMPINES", 1.3496, 103.9568, "east", 3.5, 250, 5900, (1984, 2020)),
    ("TOA PAYOH", 1.3343, 103.8563, "central", 4.8, 300, 6800, (1972, 2016)),
    ("WOODLANDS", 1.4382, 103.7890, "north", 3.8, 230, 4800, (1987, 2019)),
    ("YISHUN", 1.4304, 103.8354, "north", 4.3, 240, 5100, (1984, 2021)),
]

STREETS = {
    "ANG MO KIO": ["ANG MO KIO AVE 3", "ANG MO KIO AVE 10", "ANG MO KIO ST 21"],
    "BEDOK": ["BEDOK NTH RD", "BEDOK RESERVOIR RD", "NEW UPP CHANGI RD"],
    "BISHAN": ["BISHAN ST 12", "BISHAN ST 22", "SIN MING RD"],
    "BUKIT BATOK": ["BT BATOK WEST AVE 6", "BT BATOK ST 21", "BT BATOK EAST AVE 4"],
    "BUKIT MERAH": ["HENDERSON RD", "TELOK BLANGAH CRES", "JLN BUKIT MERAH"],
    "BUKIT PANJANG": ["PETIR RD", "PENDING RD", "SEGAR RD"],
    "CENTRAL AREA": ["ROWELL RD", "CANTONMENT RD", "SELEGIE RD"],
    "CHOA CHU KANG": ["CHOA CHU KANG AVE 3", "CHOA CHU KANG CRES", "TECK WHYE LANE"],
    "CLEMENTI": ["CLEMENTI AVE 3", "CLEMENTI WEST ST 2", "WEST COAST RD"],
    "GEYLANG": ["ALJUNIED CRES", "EUNOS CRES", "UBI AVE 1"],
    "HOUGANG": ["HOUGANG AVE 8", "HOUGANG ST 51", "UPP SERANGOON RD"],
    "JURONG EAST": ["JURONG EAST ST 21", "TEBAN GDNS RD", "TOH GUAN RD"],
    "JURONG WEST": ["JURONG WEST ST 61", "BOON LAY DR", "JURONG WEST AVE 1"],
    "KALLANG/WHAMPOA": ["WHAMPOA DR", "BENDEMEER RD", "GEYLANG BAHRU"],
    "MARINE PARADE": ["MARINE DR", "MARINE CRES", "MARINE TER"],
    "PASIR RIS": ["PASIR RIS ST 21", "PASIR RIS DR 6", "ELIAS RD"],
    "PUNGGOL": ["PUNGGOL FIELD", "EDGEFIELD PLAINS", "SUMANG LANE"],
    "QUEENSTOWN": ["MEI LING ST", "DAWSON RD", "COMMONWEALTH CL"],
    "SEMBAWANG": ["SEMBAWANG DR", "CANBERRA RD", "ADMIRALTY LINK"],
    "SENGKANG": ["COMPASSVALE RD", "FERNVALE RD", "ANCHORVALE LINK"],
    "SERANGOON": ["SERANGOON AVE 4", "SERANGOON NTH AVE 1", "LOR LEW LIAN"],
    "TAMPINES": ["TAMPINES ST 21", "TAMPINES AVE 9", "TAMPINES ST 81"],
    "TOA PAYOH": ["LOR 1 TOA PAYOH", "TOA PAYOH CTRL", "LOR 8 TOA PAYOH"],
    "WOODLANDS": ["WOODLANDS DR 16", "WOODLANDS ST 81", "MARSILING RD"],
    "YISHUN": ["YISHUN RING RD", "YISHUN AVE 11", "YISHUN ST 22"],
}

SCHOOLS = {
    "ANG MO KIO": ["Ang Mo Kio Primary School", "Mayflower Primary School"],
    "BEDOK": ["Bedok Green Primary School", "Damai Primary School"],
    "BISHAN": ["Ai Tong School", "Catholic High School (Primary)"],
    "BUKIT BATOK": ["Princess Elizabeth Primary School", "Lianhua Primary School"],
    "BUKIT MERAH": ["Radin Mas Primary School", "Gan Eng Seng Primary School"],
    "BUKIT PANJANG": ["Beacon Primary School", "West View Primary School"],
    "CENTRAL AREA": ["Cantonment Primary School", "River Valley Primary School"],
    "CHOA CHU KANG": ["Teck Whye Primary School", "Concord Primary School"],
    "CLEMENTI": ["Clementi Primary School", "Nan Hua Primary School"],
    "GEYLANG": ["Geylang Methodist School (Primary)", "Maha Bodhi School"],
    "HOUGANG": ["Holy Innocents' Primary School", "Xinmin Primary School"],
    "JURONG EAST": ["Fuhua Primary School", "Yuhua Primary School"],
    "JURONG WEST": ["Westwood Primary School", "Pioneer Primary School"],
    "KALLANG/WHAMPOA": ["Hong Wen School", "Bendemeer Primary School"],
    "MARINE PARADE": ["Tao Nan School", "Haig Girls' School"],
    "PASIR RIS": ["Casuarina Primary School", "Elias Park Primary School"],
    "PUNGGOL": ["Punggol Green Primary School", "Edgefield Primary School"],
    "QUEENSTOWN": ["Queenstown Primary School", "Fairfield Methodist School (Primary)"],
    "SEMBAWANG": ["Sembawang Primary School", "Canberra Primary School"],
    "SENGKANG": ["Compassvale Primary School", "Fernvale Primary School"],
    "SERANGOON": ["Rosyth School", "Zhonghua Primary School"],
    "TAMPINES": ["Tampines Primary School", "Junyuan Primary School"],
    "TOA PAYOH": ["Pei Chun Public School", "CHIJ Primary (Toa Payoh)"],
    "WOODLANDS": ["Woodlands Primary School", "Greenwood Primary School"],
    "YISHUN": ["Northland Primary School", "Naval Base Primary School"],
}

FLAT_TYPES = [("2 ROOM", 46, 0.06), ("3 ROOM", 68, 0.28), ("4 ROOM", 93, 0.38), ("5 ROOM", 113, 0.20), ("EXECUTIVE", 142, 0.08)]
STOREYS = ["01 TO 03", "04 TO 06", "07 TO 09", "10 TO 12", "13 TO 15", "16 TO 18", "19 TO 21", "22 TO 24", "25 TO 27"]
THIS_YEAR = 2026
TODAY = date(2026, 10, 1)


def months_back(n):
    y, m = TODAY.year, TODAY.month - n
    while m <= 0:
        m += 12
        y -= 1
    return f"{y:04d}-{m:02d}"


def jitter(scale):
    return rng.uniform(-scale, scale)


def pick_type():
    r = rng.random()
    acc = 0
    for t in FLAT_TYPES:
        acc += t[2]
        if r <= acc:
            return t
    return FLAT_TYPES[-1]


with open(OUT / "towns.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["town", "latitude", "longitude", "region", "crime_rate_per_1000", "density_per_ha"])
    for t in TOWNS:
        w.writerow([t[0], t[1], t[2], t[3], t[4], t[5]])

with open(OUT / "schools.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["name", "town", "latitude", "longitude"])
    for t in TOWNS:
        for name in SCHOOLS[t[0]]:
            w.writerow([name, t[0], round(t[1] + jitter(0.006), 6), round(t[2] + jitter(0.006), 6)])

flats = []
transactions = []
flat_id = 1
for town, lat, lng, region, crime, density, psm, (ls_min, ls_max) in TOWNS:
    spread = 0.004 if town in ("CENTRAL AREA", "MARINE PARADE", "QUEENSTOWN") else 0.007
    blocks_used = set()
    for _ in range(26):
        ftype, area_base, _w = pick_type()
        if town == "CENTRAL AREA" and ftype == "EXECUTIVE":
            ftype, area_base = "4 ROOM", 93
        street = rng.choice(STREETS[town])
        block = str(rng.randint(1, 699)) + rng.choice(["", "", "", "A", "B"])
        while (block, street) in blocks_used:
            block = str(rng.randint(1, 699))
        blocks_used.add((block, street))
        lease_start = rng.randint(ls_min, ls_max)
        remaining = 99 - (THIS_YEAR - lease_start)
        floor_area = round(area_base + rng.uniform(-6, 8))
        storey = rng.choice(STOREYS[:6] if town in ("MARINE PARADE",) else STOREYS)
        storey_idx = STOREYS.index(storey)
        f_lat = round(lat + jitter(spread), 6)
        f_lng = round(lng + jitter(spread), 6)
        # Price model: town $psm x area, lease decay, storey premium, plus noise so some flats land
        # outside the fair band in both directions.
        lease_factor = 0.55 + 0.45 * (remaining / 99) ** 1.4
        storey_factor = 1 + 0.012 * storey_idx
        fair = psm * floor_area * lease_factor * storey_factor * 1.18
        flats.append([flat_id, block, street, town, ftype, floor_area, storey, lease_start, remaining, f_lat, f_lng, region])
        n_tx = rng.randint(1, 3)
        offsets = sorted(rng.sample(range(0, 18), n_tx))
        for i, off in enumerate(offsets):
            trend = 1 - 0.004 * off  # prices drifted up ~5% a year
            noise = rng.gauss(1.0, 0.075) if i == 0 else rng.gauss(1.0, 0.04)
            price = round(fair * trend * noise / 1000) * 1000
            transactions.append([flat_id, months_back(off), price])
        flat_id += 1

with open(OUT / "flats.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["id", "block", "street", "town", "flat_type", "floor_area_sqm", "storey_range", "lease_commence_year",
                "remaining_lease_years", "latitude", "longitude", "region"])
    w.writerows(flats)

with open(OUT / "transactions.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["flat_id", "month", "resale_price"])
    w.writerows(transactions)

print(f"{len(flats)} flats, {len(transactions)} transactions -> {OUT}")
