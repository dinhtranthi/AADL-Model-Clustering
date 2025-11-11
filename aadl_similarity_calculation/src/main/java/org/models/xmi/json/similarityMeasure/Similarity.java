package org.models.xmi.json.similarityMeasure;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.traverse.DepthFirstIterator;
import org.models.xmi.json.Main;
import org.models.xmi.json.graphModel.*;
import org.models.xmi.json.visualize.JGraphXVisualization;

import javax.json.JsonObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.models.xmi.json.Main.loadConfiguration;
import static org.models.xmi.json.Main.parseJsonFile;
import org.models.xmi.json.similarityMeasure.SemanticSimilarityFastText;

import javax.json.JsonValue;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.Json;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Similarity {

    private static final Lock fileLock = new ReentrantLock();

    private final static Logger logger = LogManager.getLogger(Main.class);


    public static void main(String[] args) throws IOException {
        //leggo i file json
        try {
            FileReader fileReader = new FileReader("models/selected/cluster_test_set_generic.csv");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                String[] values = line.split(",");
                String file1 = "models/output-processing/json/" + values[0];
                file1 = file1.replace("aaxl2", "json");

                JsonObject json1 = parseJsonFile(new File(file1));
                MakeCompleteGraph.buildGraphStructure(json1.toString(), "model1");
                i++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //read all file in a folder
        File folder = new File("C:\\Users\\Admin\\OneDrive\\Desktop\\osate\\workspace\\test\\xmi");
        File[] listOfFiles = folder.listFiles();
        int i = 0;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String file1 =  file.getName();
                System.out.println(file1);
                i++;
            }
        }

    }


    public static double[] calculateComplete(JsonObject json1, JsonObject json2, String model1Name, String model2Name, double w_struct, double w_sem) {
        try {
            Model model1 = MakeCompleteGraph.buildGraphStructure(json1.toString(), "model1");
            Model model2 = MakeCompleteGraph.buildGraphStructure(json2.toString(), "model2");

            Graph<Node, Connection> g1 = model1.getGraph();
            Graph<Node, Connection> g2 = model2.getGraph();

            // logger.info("Graph 1 nodes: " + g1.vertexSet().size() + ", edges: " + g1.edgeSet().size());
            // logger.info("Graph 2 nodes: " + g2.vertexSet().size() + ", edges: " + g2.edgeSet().size());

            if (model1Name.equals(model2Name)) {
                System.out.println("==========================================================================================");
                System.out.println("==========================================================================================");
                System.out.println("Models have the same name: " + model1Name);
                System.out.println("Model 1: " + json1);
                System.out.println("Model 2: " + json2);
                System.out.println("Graph 1 nodes: " + g1.vertexSet().size() + ", edges: " + g1.edgeSet().size());
                System.out.println("Graph 2 nodes: " + g2.vertexSet().size() + ", edges: " + g2.edgeSet().size());
                System.out.println("==========================================================================================");
                System.out.println("==========================================================================================");

            }

            // logger.info("Comparing complete graphs: " + model1Name + " and " + model2Name);


            // JGraphXVisualization.VisualizeGraph(g1, model1Name);
            // JGraphXVisualization.VisualizeGraph(g2, model2Name);


            return compareGraphs(g1, g2, model1Name, model2Name, "complete", w_struct, w_sem, json1, json2);

        } catch (Exception e) {
            logger.error("Error calculating complete graph similarity", e);
        }
        return new double[]{0.0, 0.0, 0.0};
    }

    private static double[] compareGraphs(Graph<Node, Connection> g1, Graph<Node, Connection> g2, String model1Name, String model2Name, String graphType, double w_struct, double w_sem, JsonObject json1, JsonObject json2) throws IOException {
        JsonObject config = loadConfiguration("src/main/resources/config_similarity_measure.json");
        String compareMode = config.getString("compareMode");
        double similarity;

        Graph<Node, Connection> mcs = findMaximumCommonSubgraph(g1, g2);
        //JGraphXVisualization.VisualizeGraph(mcs, "MCS");
        logger.info("MCS nodes: " + mcs.vertexSet().size() + ", edges: " + mcs.edgeSet().size());
        double nodeSimilarity = 0;
        double edgeSimilarity=0;
        if (mcs.vertexSet().isEmpty()) {
            similarity = 0.0;
            System.out.println("MaximumCommonSubgraph = 0, graph similarity = 0.0");
        } else {
            switch (compareMode) {
                case "max" -> {
                    nodeSimilarity = (double) mcs.vertexSet().size() / Math.max(g1.vertexSet().size(), g2.vertexSet().size());
                    edgeSimilarity = (double) mcs.edgeSet().size() / Math.max(g1.edgeSet().size(), g2.edgeSet().size());
                    similarity = (nodeSimilarity + edgeSimilarity) / 2;
                }
                case "min" -> {
                    nodeSimilarity = (double) mcs.vertexSet().size() / Math.min(g1.vertexSet().size(), g2.vertexSet().size());
                    edgeSimilarity = (double) mcs.edgeSet().size() / Math.min(g1.edgeSet().size(), g2.edgeSet().size());
                    similarity = (nodeSimilarity + edgeSimilarity) / 2;
                }
                case "average" -> {
                    double nodeSimilarityMax = (double) mcs.vertexSet().size() / Math.max(g1.vertexSet().size(), g2.vertexSet().size());
                    double edgeSimilarityMax = (double) mcs.edgeSet().size() / Math.max(g1.edgeSet().size(), g2.edgeSet().size());
                    double nodeSimilarityMin = (double) mcs.vertexSet().size() / Math.min(g1.vertexSet().size(), g2.vertexSet().size());
                    double edgeSimilarityMin = (double) mcs.edgeSet().size() / Math.min(g1.edgeSet().size(), g2.edgeSet().size());
                    nodeSimilarity = (nodeSimilarityMax + nodeSimilarityMin) / 2;
                    edgeSimilarity = (edgeSimilarityMax + edgeSimilarityMin) / 2;
                    similarity = (nodeSimilarity + edgeSimilarity) / 2;
                }
                default -> {
                    logger.error("Invalid compareMode: " + compareMode);
                    nodeSimilarity = 0.0;
                    edgeSimilarity = 0.0;
                    similarity = 0.0;
                }
            }
            logger.info("MaximumCommonSubgraph != 0, graph similarity = " + similarity);
        }
        List<String> nameList1 = getNameList(g1);
        List<String> nameList2 = getNameList(g2);

        List<String> nameList1FromJSON = getNameListFromJSON(json1);
        List<String> nameList2FromJSON = getNameListFromJSON(json2);
        
        // logger.info("nameList1 = " + nameList1);
        // logger.info("nameList2 = " + nameList2);

        double semanticSimilarity = 0.0;
        // logger.info("initial SemanticSimilarityFastText = " + semanticSimilarity);
        try {
            logger.info("Start try catch calculateSemanticSimilarity");
            semanticSimilarity = SemanticSimilarityFastText.calculateSemanticSimilarity(nameList1FromJSON, nameList2FromJSON);
            System.out.println("Calculated Semantic Similarity: " + semanticSimilarity);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in calculating semantic similarity", e);
        }

        // double semanticSimilarity = SemanticSimilarityFastText.calculateSemanticSimilarity(nameList1, nameList2);
        // logger.info("SemanticSimilarityFastText = " + semanticSimilarity);

    if (model1Name.equals(model2Name)) {
        System.out.println("==========================================================================================");
        System.out.println("==========================================================================================");
        System.out.println("Models have the same name: " + model1Name + ", Semantic Similarity: " + semanticSimilarity);
        System.out.println("nameList1 = " + nameList1);
        System.out.println("nameList2 = " + nameList2);
        System.out.println("nameList1FromJSON = " + nameList1FromJSON);
        System.out.println("nameList2FromJSON = " + nameList2FromJSON);
        System.out.println("==========================================================================================");
        System.out.println("==========================================================================================");

    }

        if (similarity > 1) {
            similarity = 1;
        }
        double averageSimilarity;
        double normalized_semanticSimilarity;
        normalized_semanticSimilarity = (1 + semanticSimilarity)/2;
        averageSimilarity = w_struct * similarity + w_sem * normalized_semanticSimilarity;
        // averageSimilarity = w_struct * similarity + w_sem * semanticSimilarity;
        // logger.info("averageSimilarity Structural and Semantic  = " + averageSimilarity);

        // return new double[]{similarity, semanticSimilarity, averageSimilarity};
        return new double[]{similarity, normalized_semanticSimilarity, averageSimilarity};
    }


    public static Graph<Node, Connection> findMaximumCommonSubgraph(Graph<Node, Connection> g1, Graph<Node, Connection> g2) throws IOException {
        Set<Node> commonVertices = new HashSet<>();
        Set<Connection> commonEdges = new HashSet<>();
        Set<Node> g2Vertices = new HashSet<>(g2.vertexSet());
        // Find common vertices
        DepthFirstIterator<Node, Connection> g1It = new DepthFirstIterator<>(g1);
        while (g1It.hasNext()) {
            Node n1 = g1It.next();
            Iterator<Node> g2Iterator = g2Vertices.iterator();
            while (g2Iterator.hasNext()) {
                Node n2 = g2Iterator.next();
                if (n1.getCategory().equals(n2.getCategory())) {
                    commonVertices.add(n1);
                    g2Iterator.remove();
                    break;
                }
            }
        }

        // Find common edges
        Set<Connection> g1Edges = new HashSet<>(g1.edgeSet());
        Set<Connection> g2Edges = new HashSet<>(g2.edgeSet());
        for (Connection e1 : g1Edges) {
            for (Iterator<Connection> g2Iterator = g2Edges.iterator(); g2Iterator.hasNext(); ) {
                Connection e2 = g2Iterator.next();
                if (e1.getType().equals(e2.getType())) {
                    Node source1 = g1.getEdgeSource(e1);
                    Node target1 = g1.getEdgeTarget(e1);
                    Node source2 = g2.getEdgeSource(e2);
                    Node target2 = g2.getEdgeTarget(e2);
                    if (commonVertices.contains(source1) && commonVertices.contains(target1)
                            && source1.getCategory().equals(source2.getCategory())
                            && target1.getCategory().equals(target2.getCategory())) {
                        commonEdges.add(e1);
                        g2Iterator.remove();
                        break;
                    }
                }
            }
        }

        // Build the maximum common subgraph
        Graph<Node, Connection> mcs = GraphTypeBuilder.<Node, Connection>directed().edgeClass(Connection.class).buildGraph();
        for (Node node : commonVertices) {
            mcs.addVertex(node);
        }
        for (Connection edge : commonEdges) {
            Node source = g1.getEdgeSource(edge);
            Node target = g1.getEdgeTarget(edge);
            if (mcs.containsVertex(source) && mcs.containsVertex(target)) {
                mcs.addEdge(source, target, edge);
            }
        }

        // Remove isolated nodes
        Set<Node> isolatedNodes = new HashSet<>();
        for (Node node : mcs.vertexSet()) {
            if (mcs.edgesOf(node).isEmpty()) {
                isolatedNodes.add(node);
            }
        }
        for (Node node : isolatedNodes) {
            mcs.removeVertex(node);
        }
        renameMcsNodes(mcs);

        return mcs;
    }
    private static void renameMcsNodes(Graph<Node, Connection> mcs) {
        for (Node node : mcs.vertexSet()) {
            // Rinomina il nodo in base al suo tipo
            String newName = String.valueOf(node.getCategory())+ " " + node.getId();  // Puoi personalizzare il nome come preferisci
            node.setName(newName);
        }
    }


    public static List<String> getNameList(Graph<Node, Connection> g) {
        List<String> nameList = new ArrayList<>();
        for (Node n : g.vertexSet()) {
            nameList.add(n.getName());
        }
        return nameList;
    }

    public static List<String> getNameListFromJSON(JsonObject jsonContent) {
        List<String> nameList = new ArrayList<>();
        extractNames(jsonContent, nameList);
        return nameList;
    }
    
    private static void extractNames(JsonValue value, List<String> nameList) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObject obj = (JsonObject) value;
                if (obj.containsKey("name")) {
                    String name = obj.getString("name");
                    name = name.replace("_", " ");
                    nameList.add(name);
                }
                for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                    extractNames(entry.getValue(), nameList);
                }
                break;
            case ARRAY:
                JsonArray array = (JsonArray) value;
                for (JsonValue item : array) {
                    extractNames(item, nameList);
                }
                break;
            default:
                break;
        }
    }

    public static JsonObject parseJsonString(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            return reader.readObject();
        }
    }


}