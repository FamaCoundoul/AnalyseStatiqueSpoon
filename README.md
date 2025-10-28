
```markdown
# 🧩 HAI913I_TP1_Part2_Spoon — Analyseur de Code Java (Console, JavaFX, Swing)

## 📖 Description du projet
Ce projet propose un analyseur statique de code Java basé sur Eclipse.  
Il permet de parcourir, analyser et visualiser la structure interne d’un projet Java sous différentes formes :

- **Mode console** pour tester le graphe ou les statistiques analytiques en renseignant le chemin du projet via le code source.
- **Interface JavaFX et Swing** pour une visualisation interactive sur bureau.

---

## ⚙️ Architecture du projet

```

HAI913I_TP1_Part2_Spoon/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── graph/
│   │   │   │   ├── SpoonParser.java            # Exécution console : Graphe d’appel
│   │   │   │   └── ClassMethodCallVisitor.java
│   │   │   ├── visiteurs/
│   │   │   │   ├── Parser.java                 # Exécution console : Statistiques globales
│   │   │   │   ├── *.java                      # Tous les visiteurs Spoon (compteurs, analyseurs)
│   │   │   ├── gui/
│   │   │   │   ├── SpoonAnalyzerGUI.java       # Interface graphique Swing (statistiques)
│   │   │   │   └── SpoonCallGraphGUI.java      # Interface graphique JavaFX (graphe)

````

---

## 🧪 1️⃣ Mode console — Graphe des appels

**Classe principale :** `graph.SpoonParser`  

**Exécution :**
```bash
mvn exec:java -Dexec.mainClass="graph.SpoonParser"
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

## 💻 3️⃣ Interface Swing — Analyse Statistique

**Classe principale :** `gui.SpoonAnalyzerGUI`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="gui.SpoonAnalyzerGUI"
```

Cette interface permet d’afficher les statistiques sous forme de tableau, avec filtrage et visualisation directe.


---

## 🕸️ 4️⃣ Interface JavaFX — Graphe d’appels

**Classe principale :** `gui.SpoonCallGraphGUI`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="gui.SpoonCallGraphGUI"
```

Permet de visualiser dynamiquement le graphe d’appel entre classes et méthodes en mode JavaFX.

---

---

## 🕸️ 4️⃣ Interface JavaFX — Graphe d’appels

**Classe principale :** `gui.SpoonCallGraphGUI`

**Exécution :**

```bash
mvn exec:java -Dexec.mainClass="gui.SpoonCallGraphGUI"
```

Permet de visualiser dynamiquement le graphe d’appel entre classes et méthodes en mode JavaFX.

---
---

🌐 5️⃣ Application Web — JDT Analyzer Web

Classe principale : webanalyzer.WebAnalyzerApplication

Exécution :

  mvn spring-boot:run

Accès : http://localhost:8081

L’application web offre :

* Un formulaire de sélection de projet

* Un affichage des statistiques globales

* Une visualisation graphique interactive du graphe d’appels avec Cytoscape.js
---

## 🧩 Auteur

👩‍💻 **Fama COUNDOUL**
Université de Montpellier — Master Génie Logiciel
TP2 — Analyse Statique de Code avec Eclipse + Spoon


