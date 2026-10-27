package com.abcde.hdbresale.gateway.client;

import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.Town;
import com.abcde.hdbresale.gateway.GeocodeResult;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.service.PostalSectorDirectory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Offline geocoder used when OneMap is not reachable or the app runs in offline mode. It knows the major
 * employment hubs (TBD-8), universities and hospitals, every HDB town centre, and postal sectors.
 */
@Component
public class Gazetteer {

    record Place(String label, GeoPoint point, List<String> aliases) {
    }

    private final List<Place> places = new ArrayList<>();
    private final TownRepository townRepository;
    private final PostalSectorDirectory postalSectors;
    private volatile boolean townsLoaded;

    public Gazetteer(TownRepository townRepository, PostalSectorDirectory postalSectors) {
        this.townRepository = townRepository;
        this.postalSectors = postalSectors;
        place("Raffles Place", 1.2840, 103.8514, "raffles place", "raffles", "cbd", "central business district");
        place("Marina Bay Financial Centre", 1.2789, 103.8536, "marina bay", "mbfc", "marina bay financial centre");
        place("Tanjong Pagar", 1.2765, 103.8455, "tanjong pagar");
        place("Shenton Way", 1.2775, 103.8486, "shenton way");
        place("One-North", 1.2994, 103.7873, "one-north", "one north", "onenorth", "fusionopolis", "biopolis");
        place("Mapletree Business City", 1.2745, 103.7995, "mapletree business city", "pasir panjang");
        place("Science Park", 1.2907, 103.7880, "science park", "singapore science park");
        place("Buona Vista", 1.3071, 103.7900, "buona vista");
        place("Jurong East (Jurong Gateway)", 1.3331, 103.7423, "jurong gateway", "jem", "westgate");
        place("Jurong Island", 1.2655, 103.6990, "jurong island");
        place("Tuas", 1.3200, 103.6500, "tuas");
        place("Changi Business Park", 1.3343, 103.9630, "changi business park", "cbp", "expo");
        place("Changi Airport", 1.3644, 103.9915, "changi airport", "airport", "changi");
        place("Tampines Regional Centre", 1.3540, 103.9450, "tampines central", "tampines hub", "tampines regional centre");
        place("Paya Lebar Quarter", 1.3180, 103.8930, "paya lebar", "plq");
        place("Orchard Road", 1.3048, 103.8318, "orchard", "orchard road", "ion orchard");
        place("Bugis", 1.3005, 103.8558, "bugis");
        place("Novena", 1.3204, 103.8438, "novena", "tan tock seng", "ttsh");
        place("Singapore General Hospital", 1.2797, 103.8352, "sgh", "singapore general hospital", "outram");
        place("Woodlands Regional Centre", 1.4370, 103.7865, "woodlands central", "woodlands regional centre", "causeway point");
        place("Seletar Aerospace Park", 1.4160, 103.8670, "seletar", "seletar aerospace");
        place("Nanyang Technological University", 1.3483, 103.6831, "ntu", "nanyang technological university", "nanyang");
        place("National University of Singapore", 1.2966, 103.7764, "nus", "national university of singapore", "kent ridge");
        place("Singapore Management University", 1.2963, 103.8502, "smu", "singapore management university");
        place("Singapore University of Technology and Design", 1.3413, 103.9638, "sutd");
        place("Singapore Polytechnic", 1.3099, 103.7775, "singapore polytechnic", "dover");
        place("Ngee Ann Polytechnic", 1.3331, 103.7759, "ngee ann", "ngee ann polytechnic", "clementi road");
        place("Alexandra Hospital", 1.2866, 103.8015, "alexandra hospital", "alexandra");
        place("HarbourFront", 1.2653, 103.8220, "harbourfront", "vivocity");
        place("Kallang", 1.3027, 103.8710, "kallang", "national stadium", "sports hub");
    }

    private void place(String label, double lat, double lng, String... aliases) {
        places.add(new Place(label, new GeoPoint(lat, lng), List.of(aliases)));
    }

    private void loadTowns() {
        if (townsLoaded) {
            return;
        }
        synchronized (this) {
            if (townsLoaded) {
                return;
            }
            for (Town t : townRepository.findAll()) {
                String name = t.getName().toLowerCase(Locale.ROOT);
                List<String> aliases = new ArrayList<>(List.of(name));
                if (name.contains("/")) {
                    aliases.addAll(List.of(name.split("/")));
                }
                places.add(new Place(titleCase(t.getName()), new GeoPoint(t.getLatitude(), t.getLongitude()), aliases));
            }
            townsLoaded = true;
        }
    }

    public Optional<GeocodeResult> lookup(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String q = normalise(query);
        if (PostalSectorDirectory.isPostalCode(q)) {
            return postalSectors.locate(q).map(p -> new GeocodeResult(
                    "Singapore " + q + " (" + postalSectors.areaName(q).orElse("approx.") + ")", p, true));
        }
        loadTowns();
        // 1. Exact alias. 2. Query contains an alias (longest wins). 3. Alias starts with the query.
        Map<Place, Integer> scores = new LinkedHashMap<>();
        for (Place p : places) {
            for (String alias : p.aliases()) {
                int score = 0;
                if (q.equals(alias)) {
                    score = 1000;
                } else if (containsWord(q, alias)) {
                    score = 500 + alias.length();
                } else if (alias.startsWith(q) && q.length() >= 3) {
                    score = 100 + q.length();
                }
                scores.merge(p, score, Math::max);
            }
        }
        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> new GeocodeResult(e.getKey().label(), e.getKey().point(), true));
    }

    /** Short list of place names for the address autocomplete on the Search screen. */
    public List<String> suggestions(String prefix, int limit) {
        loadTowns();
        String q = normalise(prefix);
        return places.stream()
                .filter(p -> q.isEmpty() || p.label().toLowerCase(Locale.ROOT).contains(q)
                        || p.aliases().stream().anyMatch(a -> a.startsWith(q)))
                .map(Place::label)
                .distinct()
                .limit(limit)
                .toList();
    }

    private static boolean containsWord(String text, String alias) {
        return (" " + text + " ").contains(" " + alias + " ");
    }

    private static String normalise(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/ -]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String titleCase(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.toLowerCase(Locale.ROOT).split(" ")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString().replace("/w", "/W");
    }
}
