package gui;

import graph.SpoonClassMethodCallVisitor;
import graph.SpoonParser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SpoonCallGraphGUI extends Application {

    private TreeView<String> treeView = new TreeView<>();
    private Pane graphPane = new Pane();
    private Label statusLabel = new Label("Prêt");
    private Map<String, VisualNode> nodeMap = new HashMap<>();
    private Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> projectGraph = new LinkedHashMap<>();
    private BorderPane mainRoot;
    private TextField searchField = new TextField();
    private boolean legendAdded = false;

    // Transform for zoom/pan
    private double scale = 1.0;
    private double mousePrevX, mousePrevY;

    // layout constants
    private static final double CLASS_RADIUS = 36;
    private static final double METHOD_RADIUS = 28;
    private static final double NODE_SPACING_X = 220;
    private static final double NODE_SPACING_Y = 90;

    private StackPane centerStack; // <-- centerStack défini ici

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Graphe d'Appel (Spoon) - Visualisation Projet");

        // ---- Left: TreeView + Details ----
        VBox leftBox = new VBox(8);
        leftBox.setPadding(new Insets(8));
        leftBox.setPrefWidth(360);

        Label treeTitle = new Label("Structure du projet");
        treeTitle.setFont(Font.font(14));
        treeView.setRoot(new TreeItem<>("Aucun projet chargé"));
        treeView.setPrefHeight(400);
        treeView.setShowRoot(true);

        searchField.setPromptText("Rechercher classe/méthode...");
        searchField.setOnKeyReleased(e -> applySearch(searchField.getText()));

        Label detailsTitle = new Label("Détails");
        detailsTitle.setFont(Font.font(14));
        TextArea detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPrefHeight(250);

        Button exportDot = new Button("Exporter en .dot");
        exportDot.setOnAction(e -> exportDot(stage));

        leftBox.getChildren().addAll(treeTitle, searchField, treeView, detailsTitle, detailsArea, exportDot);

        // ---- Center: graphPane inside a StackPane ----
        centerStack = new StackPane();
        centerStack.getChildren().add(graphPane);

        ScrollPane scrollPane = new ScrollPane(centerStack);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        enablePanAndZoom(graphPane);

        // ---- Bottom: status ----
        HBox bottom = new HBox(statusLabel);
        bottom.setPadding(new Insets(6));

        // ---- Menu ----
        MenuBar menuBar = createMenu(stage);

        // ---- BorderPane ----
        mainRoot = new BorderPane();
        mainRoot.setTop(menuBar);
        mainRoot.setLeft(leftBox);
        mainRoot.setCenter(scrollPane); // <-- centerStack est dans le ScrollPane
        mainRoot.setBottom(bottom);

        Scene scene = new Scene(mainRoot, 1400, 900);
        stage.setScene(scene);
        stage.show();

        // ---- Tree selection listener ----
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String val = newV.getValue();
                if (val.startsWith("Classe : ")) {
                    String className = val.substring("Classe : ".length());
                    layoutAndDrawSubGraph(className, null);
                    detailsArea.setText(buildClassDetails(className));
                } else if (val.startsWith("Méthode : ")) {
                    String methodName = val.substring("Méthode : ".length());
                    layoutAndDrawSubGraph(null, methodName);
                    detailsArea.setText(buildMethodDetails(methodName));
                } else if (newV == treeView.getRoot()) {
                    layoutAndDrawGraph(projectGraph);
                    detailsArea.setText("Projet: " + SpoonParser.projectPath);
                } else {
                    detailsArea.setText(val);
                }
            }
        });
    }

    // --- MENU ---
    private MenuBar createMenu(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu file = new Menu("Fichier");
        MenuItem openProj = new MenuItem("Ouvrir projet (src)...");
        openProj.setOnAction(e -> chooseProjectDir(stage));
        MenuItem exit = new MenuItem("Quitter");
        exit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(openProj, new SeparatorMenuItem(), exit);

        Menu view = new Menu("Affichage");
        MenuItem fit = new MenuItem("Réinitialiser zoom/position");
        fit.setOnAction(e -> resetView());
        view.getItems().addAll(fit, new SeparatorMenuItem());

        Menu help = new Menu("Aide");
        MenuItem about = new MenuItem("À propos");
        about.setOnAction(e -> showAlert("À propos", "Graphe d'Appel (Spoon) – Visualisation Projet\nVersion 1.0"));
        help.getItems().add(about);

        menuBar.getMenus().addAll(file, view, help);
        return menuBar;
    }

    private void chooseProjectDir(Stage stage) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choisir le dossier racine du projet (contenant src/)");
        File chosen = dc.showDialog(stage);
        if (chosen != null) {
            SpoonParser.setProjectPath(chosen.getAbsolutePath());
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> statusLabel.setText("Analyse en cours (Spoon)..."));
                    projectGraph = SpoonParser.parseProject();
                    Platform.runLater(() -> {
                        buildTreeFromProject(projectGraph);
                        layoutAndDrawGraph(projectGraph);
                        statusLabel.setText("Analyse terminée : " + chosen.getName());
                    });
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    // --- DRAW GRAPH METHODS ---
    private void layoutAndDrawSubGraph(String className, String methodName) {
        graphPane.getChildren().clear();
        nodeMap.clear();

        if (className == null && methodName == null) return;

        double startX = 140;
        double startY = 180;

        if (className != null) {
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);
            if (methods == null) return;

            VisualNode clsNode = createVisualNode(className, startX, startY, CLASS_RADIUS, NodeType.CLASS);
            nodeMap.put(className, clsNode);
            graphPane.getChildren().addAll(clsNode.view(), clsNode.label);

            int mIdx = 0;
            double methodX = startX + NODE_SPACING_X;
            double methodYStart = startY - (methods.size() - 1) * (NODE_SPACING_Y / 2.0);
            for (String m : methods.keySet()) {
                if (methodName != null && !m.equals(methodName)) continue;
                double methodY = methodYStart + mIdx * NODE_SPACING_Y;
                VisualNode methodNode = createVisualNode(m, methodX, methodY, METHOD_RADIUS, NodeType.METHOD);
                nodeMap.put(m, methodNode);
                graphPane.getChildren().addAll(methodNode.view(), methodNode.label);

                drawArrow(clsNode.getCenterX(), clsNode.getCenterY(), methodNode.getCenterX(), methodNode.getCenterY(), Color.DARKGRAY, CLASS_RADIUS, METHOD_RADIUS);

                List<SpoonClassMethodCallVisitor.MethodCall> calls = methods.get(m);
                double callX = methodNode.getCenterX() + NODE_SPACING_X;
                double callYStart = methodNode.getCenterY() - (calls.size() - 1) * (NODE_SPACING_Y / 2.0);
                for (int j = 0; j < calls.size(); j++) {
                    SpoonClassMethodCallVisitor.MethodCall call = calls.get(j);
                    double callY = callYStart + j * NODE_SPACING_Y;
                    VisualNode target = nodeMap.get(call.name);
                    if (target == null) {
                        target = createVisualNode(call.name, callX, callY, METHOD_RADIUS, NodeType.EXTERNAL);
                        nodeMap.put(call.name, target);
                        graphPane.getChildren().addAll(target.view(), target.label);
                    }
                    drawArrow(methodNode.getCenterX(), methodNode.getCenterY(), target.getCenterX(), target.getCenterY(), Color.LIGHTBLUE, METHOD_RADIUS, target.radius);
                }
                mIdx++;
            }
        } else if (methodName != null) {
            for (String cls : projectGraph.keySet()) {
                Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(cls);
                if (methods.containsKey(methodName)) {
                    layoutAndDrawSubGraph(cls, methodName);
                    break;
                }
            }
        }

        addLegend(centerStack);
    }

    private void layoutAndDrawGraph(Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> graph) {
        graphPane.getChildren().clear();
        nodeMap.clear();

        double startX = 140;
        double startY = 180;
        double classSpacingY = Math.max(120, NODE_SPACING_Y);

        int idx = 0;
        for (String className : graph.keySet()) {
            double classY = startY + idx * classSpacingY;
            VisualNode classNode = createVisualNode(className, startX, classY, CLASS_RADIUS, NodeType.CLASS);
            nodeMap.put(className, classNode);
            graphPane.getChildren().addAll(classNode.view(), classNode.label);
            idx++;
        }

        for (String className : graph.keySet()) {
            VisualNode cls = nodeMap.get(className);
            Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            int mIdx = 0;
            double methodX = cls.getCenterX() + NODE_SPACING_X;
            double methodYStart = cls.getCenterY() - (methods.size() - 1) * (NODE_SPACING_Y / 2.0);
            for (String m : methods.keySet()) {
                double methodY = methodYStart + mIdx * NODE_SPACING_Y;
                VisualNode methodNode = nodeMap.get(m);
                if (methodNode == null) {
                    methodNode = createVisualNode(m, methodX, methodY, METHOD_RADIUS, NodeType.METHOD);
                    nodeMap.put(m, methodNode);
                    graphPane.getChildren().addAll(methodNode.view(), methodNode.label);
                }
                drawArrow(cls.getCenterX(), cls.getCenterY(), methodNode.getCenterX(), methodNode.getCenterY(), Color.DARKGRAY, cls.radius, methodNode.radius);

                List<SpoonClassMethodCallVisitor.MethodCall> calls = methods.get(m);
                double callX = methodNode.getCenterX() + NODE_SPACING_X;
                double callYStart = methodNode.getCenterY() - (calls.size() - 1) * (NODE_SPACING_Y / 2.0);
                for (int j = 0; j < calls.size(); j++) {
                    SpoonClassMethodCallVisitor.MethodCall call = calls.get(j);
                    double callY = callYStart + j * NODE_SPACING_Y;
                    VisualNode target = nodeMap.get(call.name);
                    if (target == null) {
                        target = createVisualNode(call.name, callX, callY, METHOD_RADIUS, NodeType.EXTERNAL);
                        nodeMap.put(call.name, target);
                        graphPane.getChildren().addAll(target.view(), target.label);
                    }
                    drawArrow(methodNode.getCenterX(), methodNode.getCenterY(), target.getCenterX(), target.getCenterY(), Color.LIGHTBLUE, methodNode.radius, target.radius);
                }
                mIdx++;
            }
        }

        addLegend(centerStack);
    }

    // --- ADD LEGEND ---
    private void addLegend(StackPane centerStack) {
        if (legendAdded) return;

        HBox leg = new HBox(12);
        leg.setPadding(new Insets(10));
        leg.setBackground(Background.EMPTY);
        leg.setBorder(Border.EMPTY);
        Rectangle c1 = new Rectangle(14, 14, Color.ORANGE);
        Rectangle c2 = new Rectangle(14, 14, Color.PINK);
        Rectangle c3 = new Rectangle(14, 14, Color.MEDIUMPURPLE);

        leg.getChildren().addAll(c1, new Text("Classe"), c2, new Text("Méthode (interne)"), c3, new Text("Méthode (externe)"));

        StackPane.setMargin(leg, new Insets(20));
        StackPane.setAlignment(leg, Pos.TOP_LEFT);

        centerStack.getChildren().add(leg);
        legendAdded = true;
    }

    // --- PAN & ZOOM ---
    private void enablePanAndZoom(Pane pane) {
        pane.setOnScroll(event -> {
            double delta = 1.2;
            double scaleFactor = (event.getDeltaY() > 0) ? delta : 1 / delta;
            applyZoom(scaleFactor);
            event.consume();
        });

        pane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE || (e.isPrimaryButtonDown() && e.isAltDown())) {
                mousePrevX = e.getSceneX();
                mousePrevY = e.getSceneY();
                pane.setCursor(Cursor.MOVE);
            }
        });

        pane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE || (e.isPrimaryButtonDown() && e.isAltDown())) {
                double dx = e.getSceneX() - mousePrevX;
                double dy = e.getSceneY() - mousePrevY;
                pane.setTranslateX(pane.getTranslateX() + dx);
                pane.setTranslateY(pane.getTranslateY() + dy);
                mousePrevX = e.getSceneX();
                mousePrevY = e.getSceneY();
            }
        });

        pane.setOnMouseReleased(e -> pane.setCursor(Cursor.DEFAULT));
    }

    private void applyZoom(double factor) {
        scale *= factor;
        graphPane.setScaleX(scale);
        graphPane.setScaleY(scale);
    }

    private void applySearch(String text) {
        if (text == null || text.isEmpty()) {
            nodeMap.values().forEach(n -> n.view().setOpacity(1.0));
            return;
        }
        String lower = text.toLowerCase();
        nodeMap.values().forEach(n -> n.view().setOpacity(n.name.toLowerCase().contains(lower) ? 1.0 : 0.2));
    }

    private void resetView() {
        graphPane.setTranslateX(0);
        graphPane.setTranslateY(0);
        scale = 1.0;
        graphPane.setScaleX(scale);
        graphPane.setScaleY(scale);
    }

    private void exportDot(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter graphe en .dot");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier DOT", "*.dot"));
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try (FileWriter writer = new FileWriter(f)) {
                writer.write(SpoonParser.exportGraphToDot(projectGraph));
                showAlert("Export réussi", "Fichier .dot généré : " + f.getAbsolutePath());
            } catch (IOException ex) {
                showAlert("Erreur", "Impossible d'écrire le fichier .dot : " + ex.getMessage());
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // --- NODE TYPES ---
    private enum NodeType { CLASS, METHOD, EXTERNAL }

    private static class VisualNode {
        String name;
        double x, y;
        double radius;
        NodeType type;
        Rectangle view;
        Text label;
        double dragOffsetX, dragOffsetY;

        VisualNode(String name, double x, double y, double r, NodeType type) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.radius = r;
            this.type = type;
            view = new Rectangle(x - r, y - r, 2 * r, 2 * r);
            view.setArcWidth(14);
            view.setArcHeight(14);
            view.setFill(type == NodeType.CLASS ? Color.ORANGE : type == NodeType.METHOD ? Color.PINK : Color.MEDIUMPURPLE);
            view.setStroke(Color.BLACK);
            label = new Text(x - r / 1.5, y + 4, name);
        }

        Rectangle view() { return view; }

        void setDragOffset(double dx, double dy) { dragOffsetX = dx; dragOffsetY = dy; }

        void relocateTo(double nx, double ny) {
            x = nx;
            y = ny;
            view.setX(x - radius);
            view.setY(y - radius);
            label.setX(x - radius / 1.5);
            label.setY(y + 4);
        }

        double getCenterX() { return x; }
        double getCenterY() { return y; }
    }
    
 // ---------------------------
 // Création d’un VisualNode
 // ---------------------------
 private VisualNode createVisualNode(String name, double x, double y, double radius, NodeType type) {
     VisualNode node = new VisualNode(name, x, y, radius, type);

     // Drag & Drop pour réorganiser le graphe
     node.view.setOnMousePressed(e -> {
         node.setDragOffset(e.getX() - node.view.getX() - node.radius, e.getY() - node.view.getY() - node.radius);
         node.view.setCursor(Cursor.MOVE);
     });
     node.view.setOnMouseDragged(e -> {
         double newX = e.getX() - node.dragOffsetX;
         double newY = e.getY() - node.dragOffsetY;
         node.relocateTo(newX, newY);
     });
     node.view.setOnMouseReleased(e -> node.view.setCursor(Cursor.HAND));

     return node;
 }

 // ---------------------------
 // Dessiner une flèche entre deux points
 // ---------------------------
 private void drawArrow(double startX, double startY, double endX, double endY, Color color, double startRadius, double endRadius) {
     // Calculer direction et réduire pour rayon
     double dx = endX - startX;
     double dy = endY - startY;
     double len = Math.sqrt(dx * dx + dy * dy);
     if (len == 0) return;

     double ratioStart = startRadius / len;
     double ratioEnd = endRadius / len;

     double sx = startX + dx * ratioStart;
     double sy = startY + dy * ratioStart;
     double ex = endX - dx * ratioEnd;
     double ey = endY - dy * ratioEnd;

     Line line = new Line(sx, sy, ex, ey);
     line.setStroke(color);
     line.setStrokeWidth(2);

     // Flèche
     double arrowSize = 10;
     double angle = Math.atan2(ey - sy, ex - sx);
     Polygon arrowHead = new Polygon();
     arrowHead.getPoints().addAll(
             ex, ey,
             ex - arrowSize * Math.cos(angle - Math.PI / 6),
             ey - arrowSize * Math.sin(angle - Math.PI / 6),
             ex - arrowSize * Math.cos(angle + Math.PI / 6),
             ey - arrowSize * Math.sin(angle + Math.PI / 6)
     );
     arrowHead.setFill(color);

     graphPane.getChildren().addAll(line, arrowHead);
 }

 // ---------------------------
 // Construire le TreeView à partir du graphe du projet
 // ---------------------------
 private void buildTreeFromProject(Map<String, Map<String, List<SpoonClassMethodCallVisitor.MethodCall>>> graph) {
     TreeItem<String> root = new TreeItem<>("Projet");
     root.setExpanded(true);

     for (String cls : graph.keySet()) {
         TreeItem<String> clsItem = new TreeItem<>("Classe : " + cls);
         Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = graph.get(cls);
         for (String m : methods.keySet()) {
             TreeItem<String> mItem = new TreeItem<>("Méthode : " + m);
             clsItem.getChildren().add(mItem);
         }
         root.getChildren().add(clsItem);
     }
     treeView.setRoot(root);
 }

 // ---------------------------
 // Détails d’une classe sélectionnée
 // ---------------------------
 private String buildClassDetails(String className) {
     StringBuilder sb = new StringBuilder();
     sb.append("Classe : ").append(className).append("\n");
     Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);
     if (methods != null) {
         sb.append("Méthodes :\n");
         for (String m : methods.keySet()) {
             sb.append(" - ").append(m).append(" (").append(methods.get(m).size()).append(" appels)\n");
         }
     }
     return sb.toString();
 }

 // ---------------------------
 // Détails d’une méthode sélectionnée
 // ---------------------------
 private String buildMethodDetails(String methodName) {
     StringBuilder sb = new StringBuilder();
     sb.append("Méthode : ").append(methodName).append("\n");

     for (String cls : projectGraph.keySet()) {
         Map<String, List<SpoonClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(cls);
         if (methods.containsKey(methodName)) {
             sb.append("Classe : ").append(cls).append("\n");
             List<SpoonClassMethodCallVisitor.MethodCall> calls = methods.get(methodName);
             sb.append("Appels :\n");
             for (SpoonClassMethodCallVisitor.MethodCall c : calls) {
                 sb.append("  -> ").append(c.name).append("\n");
             }
         }
     }
     return sb.toString();
 }

}
