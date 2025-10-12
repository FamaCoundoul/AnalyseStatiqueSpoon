package graph;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;

import java.io.File;
import java.util.*;

/**
 * Parser Spoon :
 * Reconstruit un graphe d'appels de méthodes au format :
 * Map<className, Map<methodName, List<MethodCall>>>
 */
public class SpoonParser {

    public static String projectPath = "/Users/njap/eclipse-workspace/visitorDesignPattern";
    public static String projectSourcePath = projectPath + "/src";

    public static void setProjectPath(String p) {
        projectPath = p;
        projectSourcePath = projectPath + "/src";
    }

    /**
     * Analyse tout le projet avec Spoon et retourne la structure d'appel.
     */
    public static Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> parseProject() {
        Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> projectGraph = new LinkedHashMap<>();

        try {
            Launcher launcher = new Launcher();
            launcher.addInputResource(projectSourcePath);
            launcher.getEnvironment().setNoClasspath(true);
            launcher.buildModel();

            CtModel model = launcher.getModel();

            for (CtElement e : model.getElements(el -> el instanceof CtClass<?>)) {
                CtClass<?> ctClass = (CtClass<?>) e;
                String className = ctClass.getSimpleName();
                Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methodsMap = new LinkedHashMap<>();

                for (CtMethod<?> method : ctClass.getMethods()) {
                    String methodName = method.getSimpleName();
                    List<SpoonClassMethodCallVisitor.MethodCall> calls = new ArrayList<>();

                    // Recherche des invocations
                    method.getElements(el -> el instanceof CtInvocation<?>)
                            .forEach(invocation -> {
                                CtInvocation<?> inv = (CtInvocation<?>) invocation;
                                CtExecutableReference<?> execRef = inv.getExecutable();

                                String callName = execRef.getSimpleName();
                                String declaringType = (execRef.getDeclaringType() != null)
                                        ? execRef.getDeclaringType().getSimpleName()
                                        : "Inconnu";

                                calls.add(new SpoonClassMethodCallVisitor.MethodCall(callName, declaringType));
                            });

                    methodsMap.put(methodName, calls);
                }

                projectGraph.put(className, methodsMap);
            }

        } catch (Exception e) {
            System.err.println("Erreur Spoon : " + e.getMessage());
            e.printStackTrace();
        }

        return projectGraph;
    }

    /**
     * Affiche le graphe d'appels complet dans la console.
     */
    public static void printProjectCallGraph() {
        Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> graph = parseProject();

        System.out.println("\n===== GRAPHE D'APPEL GLOBAL (SPOON) =====\n");
        for (String className : graph.keySet()) {
            System.out.println("Classe : " + className);
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = graph.get(className);

            if (methods.isEmpty()) {
                System.out.println("  (aucune méthode trouvée)\n");
                continue;
            }

            for (String method : methods.keySet()) {
                System.out.println("  Méthode : " + method);
                List<SpoonClassMethodCallVisitor.MethodCall> calls = methods.get(method);
                if (calls.isEmpty()) {
                    System.out.println("    -> (aucun appel détecté)");
                } else {
                    for (SpoonClassMethodCallVisitor.MethodCall call : calls) {
                        System.out.println("    -> Appelle : " + call.name + " (Type : " + call.declaringClass + ")");
                    }
                }
            }
            System.out.println();
        }
        System.out.println("=============================================\n");
    }

    /**
     * 🔹 Exporte le graphe sous forme de fichier DOT (Graphviz)
     */
    public static String exportGraphToDot(Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=box, style=filled, fillcolor=mediumpurple];\n");

        // Création des noeuds
        for (String className : graph.keySet()) {
            sb.append("  \"").append(className).append("\" [shape=ellipse, fillcolor=orange];\n");
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            for (String methodName : methods.keySet()) {
                sb.append("  \"").append(methodName).append("\" [shape=box, fillcolor=pink];\n");
            }
        }

        // Création des arêtes
        for (String className : graph.keySet()) {
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            for (String methodName : methods.keySet()) {
                sb.append("  \"").append(className).append("\" -> \"").append(methodName).append("\";\n");

                List<SpoonClassMethodCallVisitor.MethodCall> calls = methods.get(methodName);
                for (SpoonClassMethodCallVisitor.MethodCall call : calls) {
                    sb.append("  \"").append(methodName).append("\" -> \"").append(call.name).append("\";\n");
                }
            }
        }

     // --- Légende ---
        sb.append("  subgraph cluster_legend {\n");
        sb.append("    label=\"Légende\";\n");
        sb.append("    fontsize=14;\n");
        sb.append("    color=black;\n");
        sb.append("    style=dashed;\n");
        sb.append("    legend_class [label=\"Classe\", shape=box, style=filled, fillcolor=orange];\n");
        sb.append("    legend_method [label=\"Méthode interne\", shape=box, style=filled, fillcolor=pink];\n");
        sb.append("    legend_external [label=\"Méthode externe\", shape=box, style=filled, fillcolor=mediumpurple];\n");
        sb.append("    legend_class -> legend_method [label=\"Contient\"];\n");
        sb.append("    legend_method -> legend_external [label=\"Appelle\"];\n");
        sb.append("  }\n");
        
        sb.append("}\n");
        
        
        return sb.toString();
    }

    public static void main(String[] args) {
        printProjectCallGraph();
    }
}
