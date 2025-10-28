Voici le code Markdown (.md) correspondant à ton texte :

```markdown
# 🧩 HAI913I_TP1_Part2_JDT — Analyseur de Code Java (Console, JavaFX, Web)

## 📖 Description du projet
Ce projet propose un analyseur statique de code Java basé sur Eclipse JDT (Java Development Tools).  
Il permet de parcourir, analyser et visualiser la structure interne d’un projet Java sous différentes formes :

- **Mode console** pour tester le graphe ou les statistiques analytiques en renseignant le chemin du projet via le code source.
- **Interface JavaFX** pour une visualisation interactive sur bureau.
- **Application web Spring Boot + Thymeleaf** pour une utilisation moderne depuis un navigateur.

---

## ⚙️ Architecture du projet

```

HAI913I_TP1_Part2_JDT/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── graph/
│   │   │   │   ├── JDTParser.java            # Exécution console : Graphe d’appel
│   │   │   │   └── ClassMethodCallVisitor.java
│   │   │   ├── visiteurs/
│   │   │   │   ├── Parser.java               # Exécution console : Statistiques globales
│   │   │   │   ├── *.java                    # Tous les visiteurs JDT (compteurs, analyseurs)
│   │   │   ├── gui/
│   │   │   │   ├── JDTAnalyzerGUI.java       # Interface graphique JavaFX (statistiques)
│   │   │   │   └── JDTCallGraphGUI.java      # Interface graphique JavaFX (graphe)
│   │   │   └── webanalyzer/
│   │   │       ├── WebAnalyzerApplication.java       # Application web Spring Boot
│   │   │       ├── controller/ProjectController.java  # Contrôleur principal
│   │   │       ├── model/GlobalStats.java
│   │   │       └── parser/Parser.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/style.css
│   │       │   ├── images/background.avif
│   │       └── templates/
│   │           ├── index.html
│   │           └── analysis.html

````

---

## 🧪 1️⃣ Mode console — Graphe des appels

**Classe principale :** `graph.JDTParser`  

**Exécution :**
```bash
mvn exec:java -Dexec.mainClass="graph.JDTParser"
````

**Résultat attendu :**
Affiche dans la console la liste des appels entre classes et méthodes sous forme de graphe logique.

**Exemple de sortie console :**

```
Classe: MethodeA
    -> appelle : méthodeB(Type)
```

---

## 📊 2️⃣ Mode console — Statistiques analytiques

**Classe principale :** `visiteurs.Parser`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="visiteurs.Parser"
```

**Données affichées :**

* Nombre total de classes
* Nombre total de méthodes
* Moyenne d’attributs/méthodes par classe
* Classes avec plus de X méthodes
* Classes contenant les méthodes les plus longues
* Etc.

**Exemple de sortie console :**

```
===== ANALYSE STATISTIQUE DU PROJET =====
Total Classes : 27
Total Méthodes : 154
Moyenne Méthodes / Classe : 5.7
Classe avec le plus de méthodes : UserManager (12)
Classes avec plus de X=2 méthodes : [Y, Z]
```

---

## 💻 3️⃣ Interface JavaFX — Analyse Statistique

**Classe principale :** `gui.JDTAnalyzerGUI`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="gui.JDTAnalyzerGUI"
```

Cette interface permet d’afficher les statistiques sous forme de tableau, avec filtrage et visualisation directe.

**Aperçu de l’interface :**
![Statistiques JavaFX](images/statistiques.png)

---

## 🕸️ 4️⃣ Interface JavaFX — Graphe d’appels

**Classe principale :** `gui.JDTCallGraphGUI`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="gui.JDTCallGraphGUI"
```

Permet de visualiser dynamiquement le graphe d’appel entre classes et méthodes en mode JavaFX.

**Aperçu du graphe interactif :**
![Graphe JavaFX](images/graphe.png)

---

## 🌐 5️⃣ Application Web — JDT Analyzer Web

**Classe principale :** `webanalyzer.WebAnalyzerApplication`

**Exécution :**

```bash
mvn spring-boot:run
```

**Accès :** [http://localhost:8081](http://localhost:8081)

L’application web offre :

* Un formulaire de sélection de projet
* Un affichage des statistiques globales
* Une visualisation graphique interactive du graphe d’appels avec Cytoscape.js

**Page d’accueil :**
![Page d'accueil Web](images/homepage.png)

**Résultats d’analyse :**
![Résultats Web](images/results.png)

---

## 🧰 Dépendances principales (`pom.xml`)

| Dépendance                    | Utilisation                     |
| ----------------------------- | ------------------------------- |
| org.eclipse.jdt.core          | Analyse syntaxique du code Java |
| commons-io                    | Manipulation de fichiers        |
| spring-boot-starter-web       | Application web REST            |
| spring-boot-starter-thymeleaf | Templates HTML                  |
| jackson-databind              | Sérialisation JSON              |
| lombok                        | Réduction du code boilerplate   |

---

## 🔧 Compilation et exécution globale

**Compilation :**

```bash
mvn clean install
```

**Exécution Web (par défaut via exec-maven-plugin) :**

```bash
mvn exec:java
```

---

## 🧩 Auteur

👩‍💻 **Fama COUNDOUL**
Université de Montpellier — Master Génie Logiciel
TP2 — Analyse Statique de Code avec Eclipse JDT

```

---

Si tu veux, je peux aussi générer une **version `.md` enrichie avec les images intégrées et emojis correctement alignés** prête à copier-coller sur GitHub.  

Veux‑tu que je fasse ça ?
```
