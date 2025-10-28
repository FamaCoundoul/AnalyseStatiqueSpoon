package webanalyzer.service;

import java.util.*;

/**
 * Service pour construire un dendrogramme hiérarchique à partir d'une matrice de couplage.
 * Compatible Java 8 (pas de Map.of, etc.).
 */
public class DendrogramService {

    private static int idCounter = 0;

    public static class Node {
        public final String id;      // identifiant unique (utile pour Cytoscape)
        public String name;         // label lisible
        public Node left;
        public Node right;
        public double distance;

        public Node(String name) {
            this.id = "n" + (idCounter++);
            this.name = name;
        }

        public Node(Node left, Node right, double distance) {
            this.id = "n" + (idCounter++);
            // créer un nom lisible pour ce cluster
            this.name = "(" + left.name + "," + right.name + ")";
            this.left = left;
            this.right = right;
            this.distance = distance;
        }
    }

    /**
     * Construit un dendrogramme par agglomération (fusion) à partir d'une map de couplage.
     * NOTE: la map fournie sera clonée/modifiée localement — passez une copie si vous la réutilisez.
     */
    public static Node buildDendrogram(Map<String, Map<String, Double>> couplingMapInput) {
        if (couplingMapInput == null || couplingMapInput.isEmpty()) return null;

        // clone de la map afin de pouvoir la modifier en toute sécurité
        Map<String, Map<String, Double>> couplingMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> e : couplingMapInput.entrySet()) {
            couplingMap.put(e.getKey(), new HashMap<String, Double>(e.getValue()));
        }

        // clusters initiaux : une Node par classe (utiliser les clés présentes)
        Map<String, Node> clusters = new LinkedHashMap<>();
        Set<String> initialKeys = new LinkedHashSet<>(couplingMap.keySet());
        // il est possible que certaines cibles existent mais pas en tant que clé principale
        for (Map<String, Double> targets : couplingMap.values()) {
            initialKeys.addAll(targets.keySet());
        }
        for (String cls : initialKeys) {
            clusters.put(cls, new Node(cls));
            // assure une entrée vide si manquante
            couplingMap.putIfAbsent(cls, new HashMap<String, Double>());
        }

        // itérer tant qu'il reste plus d'un cluster
        while (clusters.size() > 1) {
            // trouver la paire (a,b) avec couplage maximal
            String bestA = null, bestB = null;
            double bestValue = Double.NEGATIVE_INFINITY;

            for (String a : couplingMap.keySet()) {
                Map<String, Double> row = couplingMap.get(a);
                if (row == null) continue;
                for (Map.Entry<String, Double> ent : row.entrySet()) {
                    String b = ent.getKey();
                    double val = ent.getValue() == null ? 0.0 : ent.getValue();
                    if (a.equals(b)) continue;
                    if (val > bestValue) {
                        bestValue = val;
                        bestA = a;
                        bestB = b;
                    }
                }
            }

            // si aucune paire trouvée -> sortir (graph disconnected ou valeurs nulles)
            if (bestA == null || bestB == null) break;

            // créer nouveau cluster fusionné
            Node left = clusters.get(bestA);
            Node right = clusters.get(bestB);
            Node merged = new Node(left, right, bestValue);

            // enlever anciens clusters
            clusters.remove(bestA);
            clusters.remove(bestB);

            // ajouter nouveau cluster
            clusters.put(merged.name, merged);

            // mettre à jour couplingMap : calculer moyenne (linkage average)
            Map<String, Double> mergedRow = new HashMap<>();
            couplingMap.put(merged.name, mergedRow);

            // pour chaque autre cluster 'other' calculer la distance moyenne :
            List<String> others = new ArrayList<>(couplingMap.keySet());
            for (String other : others) {
                if (other.equals(bestA) || other.equals(bestB) || other.equals(merged.name)) continue;
                double v1 = getCoupling(couplingMap, bestA, other);
                double v2 = getCoupling(couplingMap, bestB, other);
                double avg = (v1 + v2) / 2.0;
                // mettre à jour les deux directions
                mergedRow.put(other, avg);
                Map<String, Double> otherRow = couplingMap.get(other);
                if (otherRow == null) {
                    otherRow = new HashMap<>();
                    couplingMap.put(other, otherRow);
                }
                otherRow.put(merged.name, avg);
            }

            // retirer les lignes/colonnes bestA et bestB
            couplingMap.remove(bestA);
            couplingMap.remove(bestB);
            for (Map<String, Double> row : couplingMap.values()) {
                row.remove(bestA);
                row.remove(bestB);
            }
        }

        // retourner la racine (le seul restant dans clusters)
        if (clusters.isEmpty()) return null;
        return clusters.values().iterator().next();
    }

    private static double getCoupling(Map<String, Map<String, Double>> map, String a, String b) {
        if (a == null || b == null) return 0.0;
        Map<String, Double> row = map.get(a);
        if (row == null) return 0.0;
        Double v = row.get(b);
        return v == null ? 0.0 : v;
    }

    /**
     * Convertit l'arbre en une liste d'éléments Cytoscape-compatible :
     * chaque élément est une Map { "data": { "id": ..., "label": ... } } pour nodes
     * et { "data": { "source": ..., "target": ..., "weight": ... } } pour edges.
     */
  
    	public static List<Map<String, Object>> toCytoscapeDendrogram(Node root, int leafSpacing, int totalHeight) {
    	    List<Map<String, Object>> elements = new ArrayList<>();
    	    if (root == null) return elements;

    	    // 1) Lister feuilles (in-order left->right)
    	    List<Node> leaves = new ArrayList<>();
    	    collectLeaves(root, leaves);

    	    if (leaves.isEmpty()) return elements;

    	    // 2) Positions des feuilles
    	    Map<String, Double> xPos = new HashMap<>();
    	    Map<String, Double> yPos = new HashMap<>();
    	    for (int i = 0; i < leaves.size(); i++) {
    	        double x = i * leafSpacing;
    	        double y = totalHeight; // bas
    	        xPos.put(leaves.get(i).id, x);
    	        yPos.put(leaves.get(i).id, y);
    	    }

    	    // 3) Trouver distance max (pour normaliser hauteur)
    	    double maxDist = findMaxDistance(root);
    	    if (maxDist <= 0) maxDist = 1.0;

    	    // 4) Calculer positions des noeuds internes (post-order)
    	    computePositions(root, xPos, yPos, totalHeight, maxDist);

    	    // 5) Créer éléments Cytoscape : noeuds (avec position) et arêtes via noeuds-jointure pour angles droits
    	    // ajouter tous les nœuds
    	    Set<String> addedNodes = new HashSet<>();
    	    List<Node> allNodes = new ArrayList<>();
    	    collectAllNodes(root, allNodes);
    	    for (Node n : allNodes) {
    	        Map<String, Object> data = new HashMap<>();
    	        data.put("id", n.id);
    	        data.put("label", n.name);
    	        Map<String, Object> nodeWrapper = new HashMap<>();
    	        nodeWrapper.put("data", data);
    	        Map<String, Object> position = new HashMap<>();
    	        position.put("x", xPos.getOrDefault(n.id, 0.0));
    	        position.put("y", yPos.getOrDefault(n.id, 0.0));
    	        nodeWrapper.put("position", position);
    	        elements.add(nodeWrapper);
    	        addedNodes.add(n.id);
    	    }

    	    // ajouter arêtes parent->child mais en deux segments via noeud-jointure
    	    for (Node parent : allNodes) {
    	        if (parent.left != null) {
    	            addRightAngleEdge(parent, parent.left, elements, xPos, yPos);
    	        }
    	        if (parent.right != null) {
    	            addRightAngleEdge(parent, parent.right, elements, xPos, yPos);
    	        }
    	    }

    	    return elements;
    	}

    	private static void collectLeaves(Node node, List<Node> leaves) {
    	    if (node == null) return;
    	    if (node.left == null && node.right == null) {
    	        leaves.add(node);
    	        return;
    	    }
    	    collectLeaves(node.left, leaves);
    	    collectLeaves(node.right, leaves);
    	}

    	private static double findMaxDistance(Node node) {
    	    if (node == null) return Double.NEGATIVE_INFINITY;
    	    double v = node.distance;
    	    double l = findMaxDistance(node.left);
    	    double r = findMaxDistance(node.right);
    	    double m = Math.max(v, Math.max(l, r));
    	    return m == Double.NEGATIVE_INFINITY ? 0.0 : m;
    	}

    	private static void computePositions(Node node, Map<String, Double> xPos, Map<String, Double> yPos,
    	                                     int totalHeight, double maxDist) {
    	    if (node == null) return;
    	    if (node.left == null && node.right == null) {
    	        // déjà positionné
    	        return;
    	    }
    	    // post-order
    	    computePositions(node.left, xPos, yPos, totalHeight, maxDist);
    	    computePositions(node.right, xPos, yPos, totalHeight, maxDist);

    	    // x = moyenne des enfants
    	    double xl = xPos.getOrDefault(node.left.id, 0.0);
    	    double xr = xPos.getOrDefault(node.right.id, 0.0);
    	    double x = (xl + xr) / 2.0;
    	    xPos.put(node.id, x);

    	    // y = mapper distance sur l'axe vertical (0 top .. totalHeight bottom)
    	    // plus la racine sera en haut si distance élevée
    	    double normalized = node.distance / maxDist; // 0..1
    	    // on laisse une marge haute de 10% et marge basse 0 (feuilles à bottom)
    	    double topMargin = totalHeight * 0.10;
    	    double usable = totalHeight * 0.80;
    	    double y = topMargin + (1.0 - normalized) * usable;
    	    yPos.put(node.id, y);
    	}

    	private static void collectAllNodes(Node node, List<Node> list) {
    	    if (node == null) return;
    	    list.add(node);
    	    collectAllNodes(node.left, list);
    	    collectAllNodes(node.right, list);
    	}

    	private static void addRightAngleEdge(Node parent, Node child, List<Map<String, Object>> elements,
    	                                      Map<String, Double> xPos, Map<String, Double> yPos) {
    	    String junctionId = "j_" + parent.id + "_" + child.id;
    	    double jx = xPos.getOrDefault(child.id, 0.0);
    	    double jy = yPos.getOrDefault(parent.id, 0.0);

    	    // noeud-jointure (invisible style possible via Cytoscape classes)
    	    Map<String, Object> dataJ = new HashMap<>();
    	    dataJ.put("id", junctionId);
    	    dataJ.put("label", ""); // pas de label
    	    Map<String, Object> nodeWrapJ = new HashMap<>();
    	    nodeWrapJ.put("data", dataJ);
    	    Map<String, Object> posJ = new HashMap<>();
    	    posJ.put("x", jx);
    	    posJ.put("y", jy);
    	    nodeWrapJ.put("position", posJ);
    	    elements.add(nodeWrapJ);

    	    // edge parent -> junction
    	    Map<String, Object> edgeData1 = new HashMap<>();
    	    edgeData1.put("source", parent.id);
    	    edgeData1.put("target", junctionId);
    	    edgeData1.put("weight", parent.distance);
    	    Map<String, Object> edgeWrap1 = new HashMap<>();
    	    edgeWrap1.put("data", edgeData1);
    	    elements.add(edgeWrap1);

    	    // edge junction -> child
    	    Map<String, Object> edgeData2 = new HashMap<>();
    	    edgeData2.put("source", junctionId);
    	    edgeData2.put("target", child.id);
    	    edgeData2.put("weight", parent.distance);
    	    Map<String, Object> edgeWrap2 = new HashMap<>();
    	    edgeWrap2.put("data", edgeData2);
    	    elements.add(edgeWrap2);
    	}

    private static void addNodeAndEdges(Node node, List<Map<String, Object>> elements) {
        if (node == null) return;

        // node entry
        Map<String, Object> data = new HashMap<>();
        data.put("id", node.id);
        data.put("label", node.name);
        Map<String, Object> nodeWrapper = new HashMap<>();
        nodeWrapper.put("data", data);
        elements.add(nodeWrapper);

        if (node.left != null) {
            // edge node -> left
            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("source", node.id);
            edgeData.put("target", node.left.id);
            edgeData.put("weight", node.distance);
            Map<String, Object> edgeWrapper = new HashMap<>();
            edgeWrapper.put("data", edgeData);
            elements.add(edgeWrapper);

            addNodeAndEdges(node.left, elements);
        }

        if (node.right != null) {
            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("source", node.id);
            edgeData.put("target", node.right.id);
            edgeData.put("weight", node.distance);
            Map<String, Object> edgeWrapper = new HashMap<>();
            edgeWrapper.put("data", edgeData);
            elements.add(edgeWrapper);

            addNodeAndEdges(node.right, elements);
        }
    }
}
