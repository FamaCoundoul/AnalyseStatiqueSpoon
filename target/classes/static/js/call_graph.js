// call_graph.js

document.addEventListener("DOMContentLoaded", function() {
    const pre = document.querySelector("pre");
    if (!pre) return;

    const dotSource = pre.textContent;
    pre.style.display = "none";

    const container = document.createElement("div");
    container.id = "graph-container";
    document.body.appendChild(container);

    // Charger Viz.js dynamiquement
    const script = document.createElement("script");
    script.src = "https://cdn.jsdelivr.net/npm/viz.js@2.1.2/viz.js";
    script.onload = function() {
        try {
            const viz = new Viz();
            viz.renderSVGElement(dotSource)
                .then(function(element) {
                    container.appendChild(element);
                    element.style.width = "90%";
                    element.style.margin = "20px auto";
                    element.style.display = "block";
                })
                .catch(error => {
                    container.innerHTML = "<p style='color:red'>Erreur de rendu du graphe</p>";
                    console.error(error);
                });
        } catch (e) {
            console.error("Viz.js non chargé : ", e);
        }
    };
    document.body.appendChild(script);
});
