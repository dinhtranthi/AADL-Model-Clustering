package org.models.xmi.json.graphModel;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.models.xmi.json.CommonFunction;

import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MakeComponentGraph {

    private static final Logger logger = Logger.getLogger(MakeComponentGraph.class.getName());
    private static final String MODEL_DIR = "output-processing/models/";

    public static void makeGraph(String JSON_DIR) {
        try {
            Files.list(Paths.get(JSON_DIR))
                    .filter(Files::isRegularFile)
                    .forEach(MakeComponentGraph::processJsonFile);
        } catch (IOException e) {
            logger.severe("Error reading directory: " + e.getMessage());
        }
    }

    private static void processJsonFile(Path file) {
        try {
            String json = Files.readString(file);
            Model model = buildGraphStructure(json, file.getFileName().toString());
            saveToFile(MODEL_DIR + model.getName() + ".txt", model.toString());
        } catch (IOException e) {
            logger.severe("Error processing JSON file: " + e.getMessage());
        }
    }

    public static Model buildGraphStructure(String json, String fileName) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject jsonObject = reader.readObject();
            return buildStructure(jsonObject, fileName);
        }
    }

    private static Model buildStructure(JsonObject jsonObject, String fileName) {
        Graph<Node, Connection> graph = new DefaultDirectedGraph<>(Connection.class);
        Map<String, Node> nodeMap = new HashMap<>();
        Node rootNode = CommonFunction.createNode(jsonObject);
        graph.addVertex(rootNode);

        String nodePath = "componentInstance.0";
        nodeMap.put(nodePath, rootNode);

        processComponentInstances(jsonObject, rootNode, graph, nodeMap, nodePath);
        processConnectionInstances(jsonObject, graph, nodeMap);

        // Uncomment if visualization is needed
       //JGraphXVisualization.VisualizeGraph(graph, fileName);

        return new Model(jsonObject.getString("name"), graph);
    }

    private static void processComponentInstances(JsonObject jsonObject, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String parentPath) {
        if (jsonObject.containsKey("componentInstance")) {
            JsonValue componentInstance = jsonObject.get("componentInstance");
            if (componentInstance instanceof JsonArray) {
                buildComponentInstances((JsonArray) componentInstance, parentNode, graph, nodeMap, parentPath);
            } else if (componentInstance instanceof JsonObject) {
                parentPath = parentPath + "/componentInstance.0";
                addNodeToGraph((JsonObject) componentInstance, parentNode, graph, nodeMap, parentPath, 0);
            }
        }
    }

    private static void buildComponentInstances(JsonArray componentInstances, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String parentPath) {
        for (int i = 0; i < componentInstances.size(); i++) {
            JsonObject instanceObject = componentInstances.getJsonObject(i);
            String nodePath = parentPath + "/componentInstance." + i;
            addNodeToGraph(instanceObject, parentNode, graph, nodeMap, nodePath, i);
        }
    }

    private static void addNodeToGraph(JsonObject instanceObject, Node parentNode, Graph<Node, Connection> graph, Map<String, Node> nodeMap, String nodePath, int index) {
        Node node = CommonFunction.createNode(instanceObject);
        nodeMap.put(nodePath, node);
        graph.addVertex(node);
        graph.addEdge(parentNode, node, new Connection(Connection.ConnectionType.father));

        if (instanceObject.containsKey("componentInstance")) {
            JsonValue nestedComponent = instanceObject.get("componentInstance");
            if (nestedComponent instanceof JsonArray) {
                buildComponentInstances((JsonArray) nestedComponent, node, graph, nodeMap, nodePath);
            } else if (nestedComponent instanceof JsonObject) {
                nodePath = nodePath + "/componentInstance.0";
                addNodeToGraph((JsonObject) nestedComponent, node, graph, nodeMap, nodePath, 0);
            }
        }
    }

    private static void processConnectionInstances(JsonObject jsonObject, Graph<Node, Connection> graph, Map<String, Node> nodeMap) {
        if (jsonObject.containsKey("connectionInstance")) {
            if (jsonObject.get("connectionInstance") instanceof JsonArray) {
                JsonArray connectionInstances = jsonObject.getJsonArray("connectionInstance");
                buildConnections(connectionInstances, graph, nodeMap);
            } else if (jsonObject.get("connectionInstance") instanceof JsonObject) {
                JsonObject connectionInstance = jsonObject.getJsonObject("connectionInstance");
                buildConnections(Json.createArrayBuilder().add(connectionInstance).build(), graph, nodeMap);
            }
        }
    }

    private static void buildConnections(JsonArray connectionInstances, Graph<Node, Connection> graph, Map<String, Node> nodeMap) {
        for (JsonObject instanceObject : connectionInstances.getValuesAs(JsonObject.class)) {
            String source = "componentInstance.0/" + cleanConnectionString(instanceObject.getString("source"));
            String destination = "componentInstance.0/" + cleanConnectionString(instanceObject.getString("destination"));





            if (!source.equals("empty")) {
                Node sourceNode = nodeMap.get(source);
                Node destinationNode = nodeMap.get(destination);

                if (sourceNode != null && destinationNode != null) {
                    graph.addEdge(sourceNode, destinationNode, new Connection(Connection.ConnectionType.connection));
                } else {
                    logger.warning("Source or destination not found in connection instance: " + instanceObject.getString("source") + " -> " + instanceObject.getString("destination"));
                }
            }
        }
    }

    private static String cleanConnectionString(String connectionString) {
        String clean = connectionString.substring(2).replace("@", "");
        String[] split = clean.split("/");
        for(int i = 0; i < split.length; i++){
            if (split[i].contains("feature")) {
                split[i] = "";
            }
            if(i!=0){
                split[i-1]= split[i-1].replace("/", "");
            }
        }
        clean = String.join("/", split);
        //remove last char
        if(clean.length() > 0 && clean.charAt(clean.length()-1) == '/') {
            clean = clean.substring(0, clean.length() - 1);
        }

        System.out.println(clean);


        // se split contiene la parola feature, allora la stringa è vuota
        return clean;

    }

    private static void saveToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Saved graph to file: " + filePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving graph to file: " + filePath, e);
        }
    }
}
