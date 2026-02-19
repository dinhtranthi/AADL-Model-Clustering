package org.models.xmi.json;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.models.xmi.json.graphModel.Node;

import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class CommonFunction {
    private final static Logger logger = LogManager.getLogger(org.discover.arch.model.Main.class);

    public static Node createNode(JsonObject jsonObject) {
        String name = jsonObject.getString("name");
        String category = jsonObject.getString("category", "unknown");

        switch (category) {
            case "thread group":
                category = "thread_group";
                break;
            case "virtual bus":
                category = "virtual_bus";
                break;
            case "virtual processor":
                category = "virtual_processor";
                break;
            case "subprogram group":
                category = "subprogram_group";
                break;
        }
        if(category.equals("unknown")){
            if(jsonObject.containsKey("feature")){
                JsonObject features = jsonObject.getJsonObject("feature");
                if(features.containsKey("type")){
                    category = features.getString("type");
                    category = category.split(":")[1];
                    if(category.equals("DataPort")){
                        category = "dataPort";
                    }
                }

            }
        }
/*        if(category.equals("DataPort")){
            if(jsonObject.containsKey("direction")){
                String direction = jsonObject.getString("direction");
                if(direction.equals("in")){
                    category = "dataPortIn";
                }else if(direction.equals("out")){
                    category = "dataPortOut";
                }
            }else {
                category = "dataPort";
            }
        }*/


        return new Node(name, Node.Category.valueOf(category));
    }

    static void writeToFile(String fileName, String content) {
        try {
            Files.writeString(Paths.get(fileName), content + "\n", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            logger.info("Model saved to file: " + fileName);
        } catch (IOException e) {
            logger.error("Error writing model to file: " + e.getMessage());
        }
    }

    public static String getGit(String key) {
        String csvFileMap = "models/map2.csv";
        // Read the first column of csvFileMap into mapList
        HashMap<String, String> mapGit = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileMap))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String[] values1 = values[0].split("\\\\");
                String[] values2 = values[1].split("\\\\");
                values2 = values2[values2.length - 1].split("\\.");
                mapGit.put(values2[0], values1[values1.length - 1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapGit.get(key);
    }
}
