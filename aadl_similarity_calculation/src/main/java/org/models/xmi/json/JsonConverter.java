package org.models.xmi.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonConverter {

    private final static Logger logger = LogManager.getLogger(org.discover.arch.model.Main.class);

    static public void ConvertToJson(String xmiFolderPath, String jsonFolderPath) throws IOException {
        Path path = Paths.get(jsonFolderPath);
        //create directory if not exists
        if (!Files.exists(path)) {
            logger.info("Creating directory: " + path);
            Files.createDirectories(path);
        }

        File folderXmi = new File(xmiFolderPath);
        // Check if input the folder exists
        if (folderXmi.exists() && folderXmi.isDirectory()) {
            File[] filesXmi = folderXmi.listFiles();
            // Check if the folder is not empty
            assert filesXmi != null;
            // Loop through all the files in the folder
            for (File file : filesXmi) {
                if (file.isFile()) {
                    // Read the XML file
                    String xml = new String(Files.readAllBytes(file.toPath()));
                    logger.info("Converting " + file.getName() + " to JSON");
                    // Convert XML to JSON
                    XmlMapper xmlMapper = new XmlMapper();
                    JsonNode node = xmlMapper.readTree(xml.getBytes());
                    ObjectMapper jsonMapper = new ObjectMapper();

                    // Configura la formattazione del JSON
                    ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
                    String json = writer.writeValueAsString(node);
                    //remove whitespaces after ":"
                    json = json.replaceAll(":\\s+", ":");
                    // go haed after "}," and "["
                    json = json.replaceAll("},", "},\n");
                    //remove whitelines
                    json = json.replaceAll("\n", "");

                    // Rimuovi il file di estensione .aaxl2 e aggiungi .json
                    String jsonFileName = file.getName().replace(".aaxl2", ".json");
                    String jsonFilePath = jsonFolderPath + File.separator + jsonFileName;
                    Files.write(Paths.get(jsonFilePath), json.getBytes());
                    logger.info("Saved JSON to " + jsonFilePath);
                }
            }
        } else {
            logger.error("The folder " + xmiFolderPath + " does not exist. Run package osate.standalone-model before");
        }
    }
}
