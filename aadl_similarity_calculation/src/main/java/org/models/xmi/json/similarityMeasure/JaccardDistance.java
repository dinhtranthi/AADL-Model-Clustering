package org.models.xmi.json.similarityMeasure;

import org.jgrapht.Graph;
import org.models.xmi.json.visualize.JGraphXVisualization;
import org.models.xmi.json.graphModel.Connection;
import org.models.xmi.json.graphModel.MakeCompleteGraph;
import org.models.xmi.json.graphModel.Model;
import org.models.xmi.json.graphModel.Node;

import javax.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JaccardDistance {
    private static final Logger logger = Logger.getLogger(Similarity.class.getName());

    public static double calculate(JsonObject json1, JsonObject json2) {
        try {
            Model model1 = MakeCompleteGraph.buildGraphStructure(json1.toString(), "model1");
            Model model2 = MakeCompleteGraph.buildGraphStructure(json2.toString(), "model2");

            Graph<Node, Connection> g1 = model1.getGraph();
            Graph<Node, Connection> g2 = model2.getGraph();

            JGraphXVisualization.VisualizeGraph(g1, "Graph 1");
            JGraphXVisualization.VisualizeGraph(g2, "Graph 2");

            logger.info("Comparing graphs: " + model1.getName() + " and " + model2.getName());

            double vertexJaccardDistance = calculateJaccardDistance(g1.vertexSet(), g2.vertexSet());
            double edgeJaccardDistance = calculateJaccardDistance(g1.edgeSet(), g2.edgeSet());

            double similarity = (vertexJaccardDistance + edgeJaccardDistance) / 2;

            logger.info("Graph similarity based on Jaccard distance: " + similarity);

            return similarity;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error calculating similarity", e);
            return 0.0;
        }
    }

    private static <T> double calculateJaccardDistance(Set<T> set1, Set<T> set2) {
        Set<T> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<T> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        double jaccardIndex = (double) intersection.size() / union.size();
        return 1.0 - jaccardIndex;
    }
}


