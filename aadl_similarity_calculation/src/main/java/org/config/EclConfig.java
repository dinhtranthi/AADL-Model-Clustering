package org.config;

import lombok.Data;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.utils.Utils;

/**
 * @author Mauro Sonzogni
 */
@Data
public class EclConfig {

    private final static String eclConfigFilePath = "/ecl.config_similarity_measure.json";

    private final static Logger logger = LogManager.getLogger(EclConfig.class);

    // if you want to skip all ecl operation set it to true in ecl.config_similarity_measure.json
    private Boolean enabled = false;

    private String eclScriptsFolderPath;
    private String eclScriptName;
    private String csvFileFolderPath;
    private String csvFileName;
    private EclParams eclParams;

    public EclConfig() throws Exception {
        try {
            logger.info("Confiuring ECL...");
            JSONObject eclConfiguration = Utils.readJSONFile(eclConfigFilePath);

            this.enabled = eclConfiguration.getBoolean("enabled");
            // if user don't want perform ecl is useless do thinghs
            if (enabled) {
                this.eclScriptsFolderPath = eclConfiguration.getString("eclScriptsFolderPath");
                this.eclScriptName = eclConfiguration.getString("eclScriptName");
                this.csvFileFolderPath = eclConfiguration.getString("csvFileFolderPath");
                this.csvFileName = eclConfiguration.getString("csvFileName");

                // Configure ecl params
                JSONObject eclParamsObject = eclConfiguration.getJSONObject("eclParams");

                this.eclParams = new EclParams();

                if (eclParamsObject.getDouble("threshold") > 0) {
                    this.eclParams.setThreshold(eclParamsObject.getDouble("threshold"));
                } else {
                    logger.info("Threshold must be greater than 0");
                    throw new Exception(
                            "Threshold must be greater than 0, please check ecl.config_similarity_measure.json file");
                }

                this.eclParams.setComponentWeigth(eclParamsObject.getDouble("componentWeigth"));
                this.eclParams.setConnectionWeigth(eclParamsObject.getDouble("connectionWeigth"));
                this.eclParams.setFeatureWeigth(eclParamsObject.getDouble("featureWeigth"));
                this.eclParams.setFlowSpecificationWeigth(eclParamsObject.getDouble("flowSpecificationWeigth"));
                // if all is false, is useless perfor analisys
                if ((this.eclParams.getComponentWeigth() == 0) && (this.eclParams.getConnectionWeigth() == 0)
                        && (this.eclParams.getFeatureWeigth() == 0)
                        && (this.eclParams.getFlowSpecificationWeigth() == 0)) {
                    logger.warn("All weight are equals to 0");
                    throw new Exception(
                            "Please include at least one weight to perform analisys, see ecl.config_similarity_measure.json file");
                }
                if ((this.eclParams.getComponentWeigth() + this.eclParams.getConnectionWeigth()
                        + this.eclParams.getFeatureWeigth() + this.eclParams.getFlowSpecificationWeigth()) != 1) {
                    logger.warn("The sum of weight must be equal to 1 to avoid inconsistence");
                    throw new Exception(
                            "Please provide to fix weigths before perform analisys, see ecl.config_similarity_measure.json file");
                }

            } else {
                logger.info("ECL operations are diasbled, if you want to enable please provide to set to true field 'enabled' in ecl.config_similarity_measure.json");

            }
        } catch (Exception e) {
            logger.error(e);
        }

    }

}
