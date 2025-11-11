package org.config;

import lombok.Data;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.utils.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Mauro Sonzogni
 */
@Data
public class EgxConfig {

    private final static String egxConfigFilePath = "/egx.config_similarity_measure.json";

    private final static Logger logger = LogManager.getLogger(EgxConfig.class);

    // if you want to skip all egx operation set it to true in egx.config_similarity_measure.json
    private Boolean enabled = false;

    private String egxScriptsFolderPath;
    private String egxScriptName;
    private String eglScriptsFolderPath;
    private String eglScriptName;
    private String outputFolderPath;

    public EgxConfig() throws Exception {
        try {
            logger.info("Confiuring EGX...");
            JSONObject egxConfiguration = Utils.readJSONFile(egxConfigFilePath);

            this.enabled = egxConfiguration.getBoolean("enabled");
            // if user don't want perform ecl is useless do thinghs
            if (enabled) {
                this.egxScriptsFolderPath = egxConfiguration.getString("egxScriptsFolderPath");
                this.egxScriptName = egxConfiguration.getString("egxScriptName");
                this.eglScriptName = egxConfiguration.getString("eglScriptName");
                this.outputFolderPath = egxConfiguration.getString("outputFolderPath");
                Path path = Paths.get(this.outputFolderPath);
                // create folder if not already exists
                if (!Files.exists(path)) {
                    Files.createDirectory(path);
                }

            } else {
                logger.info(
                        "EGX operations are diasbled, if you want to enable please provide to set to true field 'enabled' in egx.config_similarity_measure.json");

            }
        } catch (Exception e) {
            logger.error(e);
        }

    }

}
