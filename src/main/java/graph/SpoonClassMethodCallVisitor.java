package graph;

import java.io.File;
import java.util.*;

/**
 * Version adaptée pour tests : peut générer un graphe simple sans analyser de vrai code.
 */
public class SpoonClassMethodCallVisitor {

    private final String className;
    private final Map<String, List<MethodCall>> methods = new LinkedHashMap<>();

    public SpoonClassMethodCallVisitor(String className) {
        this.className = className;
    }

    /**
     * Analyse un fichier Java (ici simulé pour tests)
     */
    public void analyze(File javaFile) {
        // Simule deux méthodes avec quelques appels
        List<MethodCall> calls1 = new ArrayList<>();
        calls1.add(new MethodCall("helperMethod", className)); // interne
        calls1.add(new MethodCall("externalMethod", "OtherClass")); // externe

        List<MethodCall> calls2 = new ArrayList<>();
        calls2.add(new MethodCall("println", "System")); // externe

        methods.put("doSomething", calls1);
        methods.put("compute", calls2);
    }

    public Map<String, List<MethodCall>> getMethods() {
        return methods;
    }

    public String getClassName() {
        return className;
    }

    /** Représente un appel de méthode */
    public static class MethodCall {
        public final String name;
        public final String declaringClass; // Ajouté pour compatibilité GUI

        public MethodCall(String name, String declaringClass) {
            this.name = name;
            this.declaringClass = declaringClass != null ? declaringClass : "";
        }
    }

    /** Méthode utilitaire pour créer un graphe de test complet */
    public static Map<String, Map<String, List<MethodCall>>> testGraph() {
        Map<String, Map<String, List<MethodCall>>> graph = new LinkedHashMap<>();

        SpoonClassMethodCallVisitor classA = new SpoonClassMethodCallVisitor("ClassA");
        classA.analyze(null);
        graph.put(classA.getClassName(), classA.getMethods());

        SpoonClassMethodCallVisitor classB = new SpoonClassMethodCallVisitor("ClassB");
        classB.analyze(null);
        graph.put(classB.getClassName(), classB.getMethods());

        return graph;
    }
}
