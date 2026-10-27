package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.GeoPoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The system's own postal code table: the first two digits of a Singapore postal code (the postal sector)
 * mapped to an approximate centre point. Used for the PHG proximity check so the parental address never
 * leaves the system (UC-07 special requirements, NFR-6), and as an offline geocoding fallback.
 * Accuracy is roughly 1-2 km, which is enough for a "within 4 km" estimate but is labelled as approximate.
 */
@Component
public class PostalSectorDirectory {

    private final Map<String, GeoPoint> sectors = new HashMap<>();
    private final Map<String, String> areaNames = new HashMap<>();

    public PostalSectorDirectory() {
        add("Raffles Place / Tanjong Pagar", 1.2800, 103.8500, "01", "02", "03", "04", "05", "06", "07", "08");
        add("Telok Blangah / HarbourFront", 1.2700, 103.8200, "09", "10");
        add("Pasir Panjang / Clementi", 1.2950, 103.7850, "11", "12", "13");
        add("Queenstown / Tiong Bahru", 1.2900, 103.8100, "14", "15", "16");
        add("Beach Road", 1.2950, 103.8550, "17");
        add("Middle Road / Golden Mile", 1.3000, 103.8560, "18", "19");
        add("Little India", 1.3080, 103.8520, "20", "21");
        add("Orchard", 1.3040, 103.8320, "22", "23");
        add("Tanglin / Holland", 1.3120, 103.8150, "24", "25", "26", "27");
        add("Bukit Timah", 1.3250, 103.8150, "28", "29", "30");
        add("Novena / Toa Payoh", 1.3330, 103.8500, "31", "32", "33");
        add("Macpherson / Potong Pasir", 1.3300, 103.8800, "34", "35", "36", "37");
        add("Geylang / Eunos", 1.3180, 103.8950, "38", "39", "40", "41");
        add("Katong / Marine Parade", 1.3050, 103.9050, "42", "43", "44", "45");
        add("Bedok / Upper East Coast", 1.3250, 103.9300, "46", "47", "48");
        add("Changi", 1.3600, 103.9800, "49", "50", "81");
        add("Tampines / Pasir Ris", 1.3550, 103.9450, "51", "52");
        add("Serangoon / Hougang / Punggol / Sengkang", 1.3700, 103.8900, "53", "54", "55", "82");
        add("Bishan / Ang Mo Kio", 1.3600, 103.8450, "56", "57");
        add("Upper Bukit Timah / Clementi Park", 1.3400, 103.7750, "58", "59");
        add("Jurong", 1.3400, 103.7200, "60", "61", "62", "63", "64");
        add("Bukit Batok / Choa Chu Kang / Bukit Panjang", 1.3700, 103.7550, "65", "66", "67", "68");
        add("Lim Chu Kang / Tengah", 1.4000, 103.7100, "69", "70", "71");
        add("Kranji / Woodlands", 1.4350, 103.7750, "72", "73");
        add("Yishun / Sembawang", 1.4350, 103.8300, "75", "76");
        add("Upper Thomson", 1.3800, 103.8250, "77", "78");
        add("Seletar", 1.3950, 103.8700, "79", "80");
    }

    private void add(String name, double lat, double lng, String... codes) {
        for (String code : codes) {
            sectors.put(code, new GeoPoint(lat, lng));
            areaNames.put(code, name);
        }
    }

    /** True for exactly six digits, ignoring surrounding spaces. */
    public static boolean isPostalCode(String text) {
        return text != null && text.trim().matches("\\d{6}");
    }

    /** Approximate centre of a 6-digit postal code's sector; empty for malformed codes or unknown sectors. */
    public Optional<GeoPoint> locate(String postalCode) {
        if (!isPostalCode(postalCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sectors.get(postalCode.trim().substring(0, 2)));
    }

    /** A readable area name for a postal code's sector, e.g. "Bishan / Ang Mo Kio". */
    public Optional<String> areaName(String postalCode) {
        if (!isPostalCode(postalCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(areaNames.get(postalCode.trim().substring(0, 2)));
    }
}
