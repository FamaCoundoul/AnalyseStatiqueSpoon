package gui;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface graphique d'analyse Spoon avec logique similaire à AnalyzerGUI (JavaFX).
 * - Onglets : stats par fichier / stats globales
 * - Choix de dossier
 * - Entrée utilisateur pour la valeur X
 * - Analyse avec visiteurs Spoon
 */
public class SpoonAnalyzerGUI {

    private JFrame frame;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextArea globalStatsArea;
    private JLabel statusLabel;
    private File projectFolder;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpoonAnalyzerGUI::new);
    }

    public SpoonAnalyzerGUI() {
        initGUI();
    }

    private void initGUI() {
        frame = new JFrame("Analyseur de Code Source - Spoon");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLayout(new BorderLayout());

        // Barre de menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFichier = new JMenu("Fichier");
        JMenuItem ouvrirProjet = new JMenuItem("Ouvrir projet");
        JMenuItem quitter = new JMenuItem("Quitter");
        ouvrirProjet.addActionListener(e -> chooseProjectFolder());
        quitter.addActionListener(e -> frame.dispose());
        menuFichier.add(ouvrirProjet);
        menuFichier.addSeparator();
        menuFichier.add(quitter);
        menuBar.add(menuFichier);

        JMenu menuAide = new JMenu("Aide");
        JMenuItem apropos = new JMenuItem("À propos");
        apropos.addActionListener(e ->
                JOptionPane.showMessageDialog(frame, "Analyseur Spoon\nVersion 1.0", "À propos", JOptionPane.INFORMATION_MESSAGE));
        menuAide.add(apropos);
        menuBar.add(menuAide);

        frame.setJMenuBar(menuBar);

        // Table des statistiques par fichier
        fileTable = new JTable();
        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"Fichier", "Classes", "Méthodes", "Lignes", "Packages"});
        fileTable.setModel(tableModel);

        JScrollPane tableScroll = new JScrollPane(fileTable);
        JPanel fileTabPanel = new JPanel(new BorderLayout());
        JButton detailsBtn = new JButton("Voir détails");
        detailsBtn.addActionListener(e -> showFileDetails());
        fileTabPanel.add(tableScroll, BorderLayout.CENTER);
        fileTabPanel.add(detailsBtn, BorderLayout.SOUTH);

        // Zone de statistiques globales
        globalStatsArea = new JTextArea();
        globalStatsArea.setEditable(false);
        globalStatsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane globalScroll = new JScrollPane(globalStatsArea);

        // Onglets
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Statistiques par fichier", fileTabPanel);
        tabbedPane.addTab("Statistiques globales", globalScroll);

        // Barre d’état
        statusLabel = new JLabel("Prêt.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void chooseProjectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choisir le projet Java à analyser");
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            projectFolder = chooser.getSelectedFile();

            // Demande de la valeur X à l'utilisateur
            String input = JOptionPane.showInputDialog(
                    frame,
                    "Entrer la valeur X (classes avec plus de X méthodes seront comptées) :",
                    "3"
            );

            if (input == null || input.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Aucune valeur entrée. Analyse annulée.",
                        "Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int threshold = Integer.parseInt(input);
                analyzeProject(projectFolder, threshold);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Valeur invalide. Veuillez entrer un nombre entier.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void analyzeProject(File folder, int threshold) {
        tableModel.setRowCount(0);
        globalStatsArea.setText("");
        statusLabel.setText("Analyse en cours...");

        List<File> javaFiles = listJavaFilesForFolder(folder);
        if (javaFiles.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Aucun fichier Java trouvé.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int totalClasses = 0, totalMethods = 0, totalLines = 0;
        Map<String, Integer> classMethodMap = new HashMap<>();
        Map<String, Integer> classAttrMap = new HashMap<>();

        try {
            for (File file : javaFiles) {
                Launcher launcher = new Launcher();
                launcher.addInputResource(file.getAbsolutePath());
                launcher.getEnvironment().setNoClasspath(true);
                launcher.buildModel();
                CtModel model = launcher.getModel();

                List<CtClass<?>> classes = model.getElements(e -> e instanceof CtClass);
                List<CtMethod<?>> methods = model.getElements(e -> e instanceof CtMethod);
                List<CtPackage> packages = model.getElements(e -> e instanceof CtPackage);

                int fileClassCount = classes.size();
                int fileMethodCount = methods.size();
                int fileLineCount = classes.stream()
                        .mapToInt(c -> c.toString().split("\n").length).sum();

                totalClasses += fileClassCount;
                totalMethods += fileMethodCount;
                totalLines += fileLineCount;

                for (CtClass<?> c : classes) {
                    classMethodMap.put(c.getSimpleName(), c.getMethods().size());
                    classAttrMap.put(c.getSimpleName(), c.getFields().size());
                }

                tableModel.addRow(new Object[]{
                        file.getName(),
                        fileClassCount,
                        fileMethodCount,
                        fileLineCount,
                        Math.max(0, packages.size() - 1)
                });
            }

            int totalPackages = countPackages(folder);
            List<String> top10Methods = topPercent(classMethodMap, 10);
            List<String> top10Attrs = topPercent(classAttrMap, 10);
            Set<String> intersection = new HashSet<>(top10Methods);
            intersection.retainAll(top10Attrs);

            visiteurs.PlusXMethodCounterVisitorSpoon xVisitor = new visiteurs.PlusXMethodCounterVisitorSpoon();
            List<String> classesOver = xVisitor.analyze(buildModel(folder), threshold);

            globalStatsArea.setText(
                    "=== STATISTIQUES GLOBALES ===\n" +
                            "Total classes / interfaces  : " + totalClasses + "\n" +
                            "Total méthodes  : " + totalMethods + "\n" +
                            "Total lignes    : " + totalLines + "\n" +
                            "Total packages  : " + totalPackages + "\n\n" +
                            "Top 10% classes (méthodes) : " + top10Methods + "\n" +
                            "Top 10% classes (attributs): " + top10Attrs + "\n" +
                            "Intersection               : " + intersection + "\n\n" +
                            "Classes > " + threshold + " méthodes : " + classesOver + "\n"
            );

            statusLabel.setText("Analyse terminée (" + javaFiles.size() + " fichiers).");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Erreur lors de l'analyse : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Erreur pendant l'analyse.");
        }
    }

    private CtModel buildModel(File folder) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(new File(folder, "src").getAbsolutePath());
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();
        return launcher.getModel();
    }

    private void showFileDetails() {
        int row = fileTable.getSelectedRow();
        if (row >= 0) {
            String fileName = (String) tableModel.getValueAt(row, 0);
            int classes = (int) tableModel.getValueAt(row, 1);
            int methods = (int) tableModel.getValueAt(row, 2);
            int lines = (int) tableModel.getValueAt(row, 3);
            int packages = (int) tableModel.getValueAt(row, 4);

            JOptionPane.showMessageDialog(frame,
                    "Fichier : " + fileName + "\n" +
                            "Classes  : " + classes + "\n" +
                            "Méthodes : " + methods + "\n" +
                            "Lignes   : " + lines + "\n" +
                            "Packages : " + packages,
                    "Détails du fichier",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier.");
        }
    }

    private List<File> listJavaFilesForFolder(File folder) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return javaFiles;
        for (File f : files) {
            if (f.isDirectory()) javaFiles.addAll(listJavaFilesForFolder(f));
            else if (f.getName().endsWith(".java")) javaFiles.add(f);
        }
        return javaFiles;
    }

    private int countPackages(File folder) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(folder.getAbsolutePath());
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();
        return Math.max(0, launcher.getModel().getAllPackages().size() - 1);
    }

    private List<String> topPercent(Map<String, Integer> map, int percent) {
        if (map.isEmpty()) return Collections.emptyList();
        int limit = Math.max(1, (int) Math.ceil(map.size() * percent / 100.0));
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
