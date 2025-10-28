package webanalyzer.controller;

import graph.SpoonParser;
import webanalyzer.parser.Parser;
import webanalyzer.service.DendrogramService;
import webanalyzer.service.ModuleIdentifierService;
import graph.SpoonClassMethodCallVisitor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ProjectControllerSpoon {
	
	private Parser parser;

    @GetMapping("/")
    public String spoonIndex() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyzeWithSpoon(@RequestParam("path") String path,
            @RequestParam(value = "classA", required = false) String classA,
            @RequestParam(value = "classB", required = false) String classB,
            @RequestParam(value = "activeTab", required = false) String activeTab,
             @RequestParam(value = "threshold", required = false) Double thresholdParam,
            Model model) {
    	
    	parser = new Parser();
    	
    	try {
			parser.analyzeProject(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Vérification du dossier
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            model.addAttribute("error", "Le chemin n'existe pas ou n'est pas un dossier : " + path);
            return "analysis";
        }
        
        List<File> javaFiles = Parser.listJavaFiles(folder);
        if (javaFiles.isEmpty()) {
            model.addAttribute("error", "Aucun fichier Java trouvé dans : " + path);
            return "analysis";
        }

        // Analyse avec Spoon
        SpoonParser.setProjectPath(path);
        Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> classMethodCalls = SpoonParser.parseProject();

        // Calcul du couplage
        Map<String, Map<String, Double>> couplingMap = calculateCouplingRatio(classMethodCalls);
        String couplingGraphJson = convertCouplingToJson(couplingMap);
        List<Map<String, Object>> couplingMatrix = buildCouplingMatrix(couplingMap);

     
     // Liste pour affichage (noms simples)
        List<String> classAliases = javaFiles.stream()
            .map(this::extractClassName)
            .collect(Collectors.toList());
        
     // On garde aussi les fichiers complets pour le calcul réel
        Map<String, String> classAliasToPath = new HashMap<>();
        for (File file : javaFiles) {
            classAliasToPath.put(extractClassName(file), file.getAbsolutePath());
        }
        
     
        	// --- Données Couplage ---
            
            model.addAttribute("couplingGraphJson", couplingGraphJson);
            model.addAttribute("couplingMatrix", couplingMatrix);
            model.addAttribute("allClasses", classAliases);
            model.addAttribute("projectPath", path);
           
        
        
        // Modules (identification)
        double threshold = (thresholdParam != null) ? thresholdParam : 0.05;
        List<Set<String>> modules = ModuleIdentifierService.identifyModules(couplingMap, threshold);

        // Dendrogramme
        DendrogramService.Node root = DendrogramService.buildDendrogram(new HashMap<>(couplingMap));
        List<Map<String, Object>> dendroElements = DendrogramService.toCytoscapeDendrogram(root, 120, 600);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String dendrogramJson = mapper.writeValueAsString(dendroElements);
            model.addAttribute("dendrogramJson", dendrogramJson);
        } catch (Exception e) {
            model.addAttribute("dendrogramJson", "[]");
        }
        
     // --- Si l’utilisateur a sélectionné deux classes ---
        if (classA != null && classB != null) {
            double couplingAB = couplingMap.getOrDefault(classA, Collections.emptyMap())
                                           .getOrDefault(classB, 0.0);
            double couplingBA = couplingMap.getOrDefault(classB, Collections.emptyMap())
                                           .getOrDefault(classA, 0.0);

            model.addAttribute("classA", classA);
            model.addAttribute("classB", classB);
            model.addAttribute("couplingResultAB", String.format("%.5f", couplingAB));
            model.addAttribute("couplingResultBA", String.format("%.5f", couplingBA));
        } else {
        	model.addAttribute("classA",classA);
        	model.addAttribute("classB",classB);
        	
            model.addAttribute("couplingResultAB", "—");
            model.addAttribute("couplingResultBA", "—");
        }

        // Envoi au modèle
        model.addAttribute("couplingGraphJson", couplingGraphJson);
        model.addAttribute("couplingMatrix", couplingMatrix);
        model.addAttribute("modules", modules);
        model.addAttribute("threshold", threshold);
        model.addAttribute("projectPath", path);
       // Restaure l’onglet actif après soumission
        if (activeTab == null || activeTab.isEmpty()) {
            activeTab = "CouplageClasse"; // par défaut
        }
        model.addAttribute("activeTab", activeTab);

        return "analysis";
    }

    // --------------------------------------------------------------
    // === Méthodes utilitaires (copiées depuis ProjectController) ===
    // --------------------------------------------------------------

    private Map<String, Map<String, Double>> calculateCouplingRatio(
            Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> classMethodCalls) {

        Map<String, Map<String, Double>> couplingMap = new HashMap<>();
        int totalRelations = 0;

        for (String sourceClass : classMethodCalls.keySet()) {
            for (List<SpoonClassMethodCallVisitor.MethodCall> calls : classMethodCalls.get(sourceClass).values()) {
                for (SpoonClassMethodCallVisitor.MethodCall call : calls) {
                    if (!isInternalCall(sourceClass, call.declaringClass)) totalRelations++;
                }
            }
        }

        for (String sourceClass : classMethodCalls.keySet()) {
            for (List<SpoonClassMethodCallVisitor.MethodCall> calls : classMethodCalls.get(sourceClass).values()) {
                for (SpoonClassMethodCallVisitor.MethodCall call : calls) {
                    if (!isInternalCall(sourceClass, call.declaringClass)) {
                        couplingMap
                                .computeIfAbsent(sourceClass, k -> new HashMap<>())
                                .merge(call.declaringClass, 1.0, Double::sum);
                    }
                }
            }
        }

        if (totalRelations > 0) {
            for (String a : couplingMap.keySet()) {
                for (String b : couplingMap.get(a).keySet()) {
                    double value = couplingMap.get(a).get(b) / totalRelations;
                    couplingMap.get(a).put(b, value);
                }
            }
        }

        return couplingMap;
    }

    private boolean isInternalCall(String sourceClass, String targetType) {
        if (targetType == null) return true;
        return sourceClass.equalsIgnoreCase(targetType.replace(".java", ""));
    }

    private String convertCouplingToJson(Map<String, Map<String, Double>> couplingMap) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode elements = mapper.createArrayNode();
            Set<String> allClasses = new HashSet<>(couplingMap.keySet());
            couplingMap.values().forEach(targets -> allClasses.addAll(targets.keySet()));

            for (String cls : allClasses) {
                ObjectNode node = mapper.createObjectNode();
                node.putObject("data").put("id", cls).put("label", cls);
                elements.add(node);
            }

            for (String a : couplingMap.keySet()) {
                for (Map.Entry<String, Double> entry : couplingMap.get(a).entrySet()) {
                    ObjectNode edge = mapper.createObjectNode();
                    edge.putObject("data")
                            .put("source", a)
                            .put("target", entry.getKey())
                            .put("weight", entry.getValue());
                    elements.add(edge);
                }
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(elements);
        } catch (Exception e) {
            return "[]";
        }
    }

 // java
    private List<Map<String, Object>> buildCouplingMatrix(Map<String, Map<String, Double>> couplingMap) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String a : couplingMap.keySet()) {
            Map<String, Double> targets = couplingMap.get(a);
            if (targets == null) continue;
            for (String b : targets.keySet()) {
                Map<String, Object> m = new HashMap<>();
                // Utiliser les clés attendues par le template Thymeleaf
                m.put("source", a);
                m.put("target", b);
                m.put("value", targets.get(b));
                list.add(m);
            }
        }
        return list;
    }

    
    private String extractClassName(File file) {
        String name = file.getName();
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }
}
