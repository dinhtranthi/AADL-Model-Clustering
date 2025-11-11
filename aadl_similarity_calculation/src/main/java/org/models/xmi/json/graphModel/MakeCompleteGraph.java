package org.models.xmi.json.graphModel;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.models.xmi.json.similarityMeasure.Similarity;
import org.models.xmi.json.visualize.JGraphXVisualization;

import javax.json.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


import static org.models.xmi.json.CommonFunction.createNode;
import static org.models.xmi.json.Main.parseJsonFile;

public class MakeCompleteGraph {

     public static void main(String[] args) throws IOException {
         //read json from file
         String path ="models/output-processing/json/890-isolette_KSU_Isolette_thermostat_single_sensor_impl_6.json";
         String path2 = "models/output-processing/json/890-isolette_KSU_Isolette_thermostat_single_sensor_impl_7.json";
         File file = new File(path);
         File file2 = new File(path2);
         JsonObject jsonObject = parseJsonFile(file);
         JsonObject jsonObject2 = parseJsonFile(file2);

         buildStructure(jsonObject, file.getName());
         buildStructure(jsonObject2, file2.getName());
         Similarity.calculateComplete(jsonObject, jsonObject2, file.getName(), file2.getName(),1,0);




    }

    @NotNull
    public static Model buildGraphStructure(String json, String fileName) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject jsonObject = reader.readObject();
            return buildStructure(jsonObject, fileName);
        }
    }

    @NotNull
    @Contract("_, _ -> new")
    private static Model buildStructure(JsonObject jsonObject, String fileName) {
        Graph<Node, Connection> graph = new DefaultDirectedGraph<>(Connection.class);
        Map<String, Node> nodeMap = new HashMap<>();
        Node rootNode = createNode(jsonObject);
        graph.addVertex(rootNode);

        String nodePath = "componentInstance.0";
        nodeMap.put(nodePath, rootNode);

        processInstances(jsonObject, rootNode, graph, nodeMap, nodePath);
        processConnectionInstances(jsonObject, graph, nodeMap);

        //JGraphXVisualization.VisualizeGraph(graph, fileName);
        return new Model(jsonObject.getString("name"), graph);
    }

    private static void processInstances(JsonObject jsonObject, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String parentPath) {
        if (jsonObject.containsKey("componentInstance")) {
            JsonValue componentInstance = jsonObject.get("componentInstance");
            if (componentInstance instanceof JsonArray) {
                processComponentInstances((JsonArray) componentInstance, parentNode, graph, nodeMap, parentPath);
            } else if (componentInstance instanceof JsonObject) {
                String nodePath = parentPath + "/componentInstance.0";
                addNodeToGraph((JsonObject) componentInstance, parentNode, graph, nodeMap, nodePath);
            }
        }

        if (jsonObject.containsKey("featureInstance")) {
            JsonValue featureInstance = jsonObject.get("featureInstance");
            if (featureInstance instanceof JsonArray) {
                processFeatureInstances((JsonArray) featureInstance, parentNode, graph, nodeMap, parentPath);
            } else if (featureInstance instanceof JsonObject) {
                String nodePath = parentPath + "/featureInstance.0";
                addNodeToGraph((JsonObject) featureInstance, parentNode, graph, nodeMap, nodePath);
            }
        }
    }

    private static void processComponentInstances(JsonArray componentInstances, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String parentPath) {
        for (int i = 0; i < componentInstances.size(); i++) {
            JsonObject instanceObject = componentInstances.getJsonObject(i);
            String nodePath = parentPath + "/componentInstance." + i;
            addNodeToGraph(instanceObject, parentNode, graph, nodeMap, nodePath);
        }
    }

    private static void processFeatureInstances(JsonArray featureInstances, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String parentPath) {
        for (int i = 0; i < featureInstances.size(); i++) {
            JsonObject featureObject = featureInstances.getJsonObject(i);
            String nodePath = parentPath + "/featureInstance." + i;
            addNodeToGraph(featureObject, parentNode, graph, nodeMap, nodePath);
        }
    }

    private static void addNodeToGraph(JsonObject instanceObject, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String nodePath) {
        Node node = createNode(instanceObject);
        nodeMap.put(nodePath, node);
        graph.addVertex(node);
        if (node.getType().equals(Node.Type.unknown)) {
            node.setCategory(Node.Category.unknown);
            node.setType(Node.Type.unknown);

        }
        Connection.ConnectionType connectionType = (node.getType().equals(Node.Type.feature)) ? Connection.ConnectionType.feature : Connection.ConnectionType.father;
        graph.addEdge(parentNode, node, new Connection(connectionType));


        processInstances(instanceObject, node, graph, nodeMap, nodePath);
    }

    private static void processConnectionInstances(JsonObject jsonObject, Graph<Node, Connection> graph, Map<String, Node> nodeMap) {
        if (jsonObject.containsKey("connectionInstance")) {
            if (jsonObject.get("connectionInstance") instanceof JsonArray) {
                JsonArray connectionInstances = jsonObject.getJsonArray("connectionInstance");
                buildConnections(connectionInstances, graph, nodeMap);
            } else if (jsonObject.get("connectionInstance") instanceof JsonObject) {
                JsonObject connectionInstance = jsonObject.getJsonObject("connectionInstance");
                buildConnection(connectionInstance, graph, nodeMap);
            }
        }
    }

    private static void buildConnection(JsonObject connectionInstance, Graph<Node, Connection> graph, Map<String, Node> nodeMap) {
        String source = "componentInstance.0/" + cleanConnectionString(connectionInstance.getString("source"));
        String destination = "componentInstance.0/" + cleanConnectionString(connectionInstance.getString("destination"));

        Node sourceNode = nodeMap.get(source);
        Node destinationNode = nodeMap.get(destination);

        if (sourceNode != null && destinationNode != null) {
            graph.addEdge(sourceNode, destinationNode, new Connection(Connection.ConnectionType.connection));
        } else {
            String error = "";
            if (sourceNode == null) {
                error += "sourceNode is null: " + source + ". ";
            }
            if (destinationNode == null) {
                error += "destinationNode is null: " + destination + ". ";
            }
            System.out.println(error);
        }
    }

    private static void buildConnections(JsonArray connectionInstances, Graph<Node, Connection> graph, Map<String, Node> nodeMap) {
        for (JsonObject instanceObject : connectionInstances.getValuesAs(JsonObject.class)) {
            buildConnection(instanceObject, graph, nodeMap);
        }
    }

    private static String cleanConnectionString(String connectionString) {
        return connectionString.substring(2).replace("@", "");
    }
}


