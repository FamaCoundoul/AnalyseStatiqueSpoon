package visiteurs;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Version Spoon de la classe Parser.
 * Analyse un projet Java et calcule les 13 indicateurs demandés.
 */
public class Parser {

    // Chemin du projet à analyser
    public final static String projectPath = "/Users/njap/eclipse-workspace/visitorDesignPattern";
    public final static String projectSourcePath = projectPath + "/src";

    public static void main(String[] args) {

        System.out.println("========== STATISTIQUES PAR FICHIER ==========");

        // --- Récupération de tous les fichiers .java ---
        List<File> javaFiles = listJavaFilesForFolder(new File(projectSourcePath));

        // Compteurs globaux
        int totalClasses = 0;
        int totalMethods = 0;
        int totalLines = 0;
        int totalPackages = 0;

        int totalFields = 0;

        Map<String, Integer> classMethodMap = new HashMap<>();
        Map<String, Integer> classAttrMap = new HashMap<>();
        Map<String, Integer> methodLengthMap = new HashMap<>();
        int maxParameters = 0;
        
        // Avant la boucle sur les fichiers
        Launcher launcherGlobal = new Launcher();
        launcherGlobal.addInputResource(projectSourcePath);
        launcherGlobal.getEnvironment().setNoClasspath(true);
        launcherGlobal.buildModel();

        CtModel globalModel = launcherGlobal.getModel();

        // Récupérer le nombre de packages distincts
        Set<String> allPackages = globalModel.getAllPackages().stream()
                .map(CtPackage::getQualifiedName)
                .collect(Collectors.toSet());

        totalPackages = allPackages.size()-1;  // <-- Nombre de packages globaux


        for (File file : javaFiles) {
            // Charger le fichier dans Spoon
            Launcher launcher = new Launcher();
            launcher.addInputResource(file.getAbsolutePath());
            launcher.getEnvironment().setNoClasspath(true);
            launcher.buildModel();

            CtModel model = launcher.getModel();

            // Classes du fichier
            List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
            List<CtMethod<?>> methods = model.getElements(new TypeFilter<>(CtMethod.class));

            // Calculs locaux
            int fileClassCount = classes.size();
            int fileMethodCount = methods.size();
            int fileLineCount = classes.stream().mapToInt(c -> c.toString().split("\n").length).sum();
            int fileAttrCount = classes.stream().mapToInt(c -> c.getFields().size()).sum();
            

            totalClasses += fileClassCount;
            totalMethods += fileMethodCount;
            totalLines += fileLineCount;
           
            totalFields += fileAttrCount;

            // Moyennes par fichier
            double avgMethodsPerClass = fileClassCount == 0 ? 0 : (double) fileMethodCount / fileClassCount;
            double avgAttrPerClass = fileClassCount == 0 ? 0 : (double) fileAttrCount / fileClassCount;
            double avgLinesPerMethod = methods.isEmpty() ? 0 :
                    methods.stream()
                            .filter(m -> m.getBody() != null)
                            .mapToInt(Parser::getMethodLength)
                            .average().orElse(0);

            // Top 10% classes par méthodes / attributs
            for (CtClass<?> c : classes) {
                classMethodMap.put(c.getSimpleName(), c.getMethods().size());
                classAttrMap.put(c.getSimpleName(), c.getFields().size());
            }

            // Méthodes longues
            for (CtMethod<?> m : methods) {
                methodLengthMap.put(m.getSimpleName(), getMethodLength(m));
                maxParameters = Math.max(maxParameters, m.getParameters().size());
            }

            // === AFFICHAGE PAR FICHIER ===
            System.out.println("Fichier analysé : " + file.getName());
            System.out.println(" -> Nombre moyen de Méthodes par classe : " + avgMethodsPerClass);
            System.out.println(" -> Nombre moyen de lignes de code par Méthode : " + avgLinesPerMethod);
            System.out.println(" -> Nombre moyen d’Attributs par classe : " + avgAttrPerClass);
            System.out.println("===========================================");
        }
        
      
        // Calculs globaux
        double avgMethodsPerClass = totalClasses == 0 ? 0 : (double) totalMethods / totalClasses;
        double avgAttrPerClass = totalClasses == 0 ? 0 : (double) totalFields / totalClasses;
        double avgLinesPerMethod = totalMethods == 0 ? 0 : (double) totalLines / totalMethods;

        List<String> top10MethodsClasses = topPercent(classMethodMap, 10);
        List<String> top10AttrsClasses = topPercent(classAttrMap, 10);

        // Intersection des deux
        Set<String> intersection = new HashSet<>(top10MethodsClasses);
        intersection.retainAll(top10AttrsClasses);

        // Classes avec plus de X méthodes
        int X = 3;
        List<String> overXMethods = classMethodMap.entrySet().stream()
                .filter(e -> e.getValue() > X)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 10% des méthodes les plus longues
        List<String> top10LongestMethods = topPercent(methodLengthMap, 10);

        // === AFFICHAGE GLOBAL ===
        System.out.println("\n========== STATISTIQUES GLOBALES ==========");
        System.out.println("Total classes & interfaces : " + totalClasses);
        System.out.println("Total méthodes : " + totalMethods);
        System.out.println("Total lignes de code : " + totalLines);
        System.out.println("Total packages : " + totalPackages);
        System.out.println(" -> Les 10% des classes avec le plus grand nombre de méthodes : " + top10MethodsClasses);
        System.out.println(" -> Les 10% des classes avec le plus grand nombre d’attributs : " + top10AttrsClasses);
        System.out.println(" -> Classes dans les deux catégories : " + intersection);
        System.out.println(" -> Classes avec plus de " + X + " méthodes : " + overXMethods);
        System.out.println(" -> 10% des méthodes les plus longues : " + top10LongestMethods);
        System.out.println(" -> Nombre maximal de paramètres d'une méthode : " + maxParameters);
    }

    // ---------- MÉTHODES UTILITAIRES ----------

    public static List<File> listJavaFilesForFolder(final File folder) {
        List<File> javaFiles = new ArrayList<>();
        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                javaFiles.addAll(listJavaFilesForFolder(fileEntry));
            } else if (fileEntry.getName().endsWith(".java")) {
                javaFiles.add(fileEntry);
            }
        }
        return javaFiles;
    }

    private static int getMethodLength(CtMethod<?> m) {
        CtBlock<?> body = m.getBody();
        if (body == null) return 0;
        return body.toString().split("\n").length;
    }

    private static List<String> topPercent(Map<String, Integer> map, int percent) {
        if (map.isEmpty()) return Collections.emptyList();
        int limit = Math.max(1, (int) Math.ceil(map.size() * percent / 100.0));
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
