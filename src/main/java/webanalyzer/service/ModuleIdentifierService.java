package webanalyzer.service;

import java.util.*;

/**
 * Service d'identification de modules à partir du graphe de couplage.
 * Chaque module est un ensemble de classes fortement liées selon un seuil donné.
 */
public class ModuleIdentifierService {

    /**
     * Identifie les modules (groupes de classes fortement couplées)
     * @param couplingMap graphe de couplage (A -> B -> poids)
     * @param threshold seuil minimal de couplage (0.0–1.0)
     * @return liste de modules (chaque module = Set<String> de classes)
     */
    public static List<Set<String>> identifyModules(Map<String, Map<String, Double>> couplingMap, double threshold) {
        // Graphe non orienté basé sur les relations de couplage
        Map<String, Set<String>> adjacency = new HashMap<>();

        for (String a : couplingMap.keySet()) {
            for (Map.Entry<String, Double> entry : couplingMap.get(a).entrySet()) {
                String b = entry.getKey();
                double value = entry.getValue();

                if (value >= threshold) {
                    adjacency.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    adjacency.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                }
            }
        }

        // Recherche des composantes connexes (DFS)
        Set<String> visited = new HashSet<>();
        List<Set<String>> modules = new ArrayList<>();

        for (String cls : adjacency.keySet()) {
            if (!visited.contains(cls)) {
                Set<String> module = new HashSet<>();
                explore(cls, adjacency, visited, module);
                modules.add(module);
            }
        }

        return modules;
    }

    private static void explore(String node, Map<String, Set<String>> adjacency,
                                Set<String> visited, Set<String> module) {
        visited.add(node);
        module.add(node);
        for (String neighbor : adjacency.getOrDefault(node, Collections.emptySet())) {
            if (!visited.contains(neighbor)) {
                explore(neighbor, adjacency, visited, module);
            }
        }
    }
}
