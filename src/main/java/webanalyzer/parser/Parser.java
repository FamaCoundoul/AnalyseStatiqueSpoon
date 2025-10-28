package webanalyzer.parser;

import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.code.*;
import spoon.reflect.visitor.filter.TypeFilter;

import graph.SpoonClassMethodCallVisitor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyseur Java basé sur Spoon : analyse complète du projet et collecte des métriques + graphe d’appels.
 */
@Service
public class Parser {

    // --- Métriques globales ---
    private int totalClasses = 0;
    private int totalInterfaces = 0;
    private int totalMethods = 0;
    private int totalAttributes = 0;
    private int totalPackage = 0;
    private int totalLines = 0;
    private int totalLinesInMethods = 0;
    private int maxParameters = 0;

    private double avgMethodsPerClass = 0.0;
    private double avgLinesPerMethod = 0.0;
    private double avgAttributesPerClass = 0.0;

    // --- Données collectées ---
    private final Map<String, FileAnalysis> fileAnalyses = new LinkedHashMap<>();
    private final Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> classMethodCalls = new LinkedHashMap<>();
    private final Map<String, Integer> methodsPerClass = new HashMap<>();
    private final Map<String, Integer> attributesPerClass = new HashMap<>();

    private String projectPath;

    // ============================
    // Sous-classe pour l'analyse fichier
    // ============================
    public static class FileAnalysis {
        private final String fileName;
        private final List<String> classes = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();

        public FileAnalysis(String fileName) { this.fileName = fileName; }
        public void addClass(String cls) { classes.add(cls); }
        public void addMethods(Collection<String> m) { methods.addAll(m); }
        public String getFileName() { return fileName; }
        public List<String> getClasses() { return classes; }
        public List<String> getMethods() { return methods; }
    }

    // ============================
    // Analyse principale du projet
    // ============================
    public void analyzeProject(String projectPath) throws IOException {
        if (projectPath == null || projectPath.isEmpty()) return;
        this.projectPath = projectPath;

        resetMetrics();

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setCommentEnabled(false);

        CtModel model = launcher.buildModel();
        Set<String> visitedPackages = new HashSet<>();

        for (CtType<?> type : model.getAllTypes()) {
            String className = type.getQualifiedName();
            FileAnalysis fa = new FileAnalysis(className + ".java");

            if (type.isInterface()) totalInterfaces++;
            else totalClasses++;

            visitedPackages.add(type.getPackage().getQualifiedName());

            // --- Compte des attributs
            int attrCount = (int) type.getFields().stream().count();
            attributesPerClass.put(className, attrCount);
            totalAttributes += attrCount;

            // --- Compte des méthodes
            int methodCount = type.getMethods().size();
            methodsPerClass.put(className, methodCount);
            totalMethods += methodCount;

            // --- Comptage des lignes de code (approximatif)
            totalLines += type.toString().split("\n").length;

            // --- Collecte des appels de méthodes (graphe)
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methodMap = new LinkedHashMap<>();

            for (CtMethod<?> m : type.getMethods()) {
                List<SpoonClassMethodCallVisitor.MethodCall> calls = new ArrayList<>();
                totalLinesInMethods += (m.getBody() != null) ? m.getBody().toString().split("\n").length : 0;

                // Paramètres max
                int paramCount = m.getParameters().size();
                if (paramCount > maxParameters) maxParameters = paramCount;

                // Rechercher les appels de méthodes
                if (m.getBody() != null) {
                    for (CtInvocation<?> inv : m.getElements(new TypeFilter<>(CtInvocation.class))) {
                        String calledName = inv.getExecutable().getSimpleName();
                        String declaringClass = inv.getExecutable().getDeclaringType() != null ?
                                inv.getExecutable().getDeclaringType().getSimpleName() : "Unknown";
                        calls.add(new SpoonClassMethodCallVisitor.MethodCall(calledName, declaringClass));
                    }
                }
                methodMap.put(m.getSimpleName(), calls);
                fa.addMethods(Collections.singleton(m.getSimpleName()));
            }

            classMethodCalls.put(className, methodMap);
            fa.addClass(className);
            fileAnalyses.put(className, fa);
        }

        totalPackage = visitedPackages.size();

        // --- Calcul des moyennes
        int totalTypes = totalClasses + totalInterfaces;
        avgMethodsPerClass = totalTypes == 0 ? 0 : (double) totalMethods / totalTypes;
        avgAttributesPerClass = totalTypes == 0 ? 0 : (double) totalAttributes / totalTypes;
        avgLinesPerMethod = totalMethods == 0 ? 0 : (double) totalLinesInMethods / totalMethods;
    }
    
    
    public static List<File> listJavaFiles(File folder) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    javaFiles.addAll(listJavaFiles(f));
                } else if (f.getName().endsWith(".java")) {
                    javaFiles.add(f);
                }
            }
        }
        return javaFiles;
    }

    // ============================
    // Méthodes utilitaires
    // ============================
    private void resetMetrics() {
        totalClasses = totalInterfaces = totalMethods = totalAttributes = totalPackage = 0;
        totalLines = totalLinesInMethods = maxParameters = 0;
        avgMethodsPerClass = avgLinesPerMethod = avgAttributesPerClass = 0.0;
        fileAnalyses.clear();
        classMethodCalls.clear();
        methodsPerClass.clear();
        attributesPerClass.clear();
    }

    // ============================
    // Accesseurs pour le contrôleur
    // ============================
    public int getTotalClasses() { return totalClasses; }
    public int getTotalInterfaces() { return totalInterfaces; }
    public int getTotalMethods() { return totalMethods; }
    public int getTotalAttributes() { return totalAttributes; }
    public int getTotalPackage() { return totalPackage; }
    public int getTotalLines() { return totalLines; }
    public int getMaxParameters() { return maxParameters; }

    public double getAvgMethodsPerClass() { return avgMethodsPerClass; }
    public double getAvgLinesPerMethod() { return avgLinesPerMethod; }
    public double getAvgAttributesPerClass() { return avgAttributesPerClass; }

    public Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> getClassMethodCalls() { return classMethodCalls; }
    public Map<String, FileAnalysis> getFileAnalyses() { return fileAnalyses; }

    public List<String> getTopMethodsClasses() {
        int limit = Math.max(1, (int) Math.ceil(0.10 * methodsPerClass.size()));
        return methodsPerClass.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getTopAttributeClasses() {
        int limit = Math.max(1, (int) Math.ceil(0.10 * attributesPerClass.size()));
        return attributesPerClass.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getIntersectionTopClasses() {
        Set<String> topMethods = new HashSet<>(getTopMethodsClasses());
        Set<String> topAttributes = new HashSet<>(getTopAttributeClasses());
        topMethods.retainAll(topAttributes);
        return new ArrayList<>(topMethods);
    }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
}
