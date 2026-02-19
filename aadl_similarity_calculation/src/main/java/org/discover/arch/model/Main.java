package org.discover.arch.model;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.Config;

public class Main {
    private static Config config = null;
    private final static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        try {
            // By now the config location file is hardcoded in Config class
            config = new Config();

        } catch (Exception e) {
            logger.error("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
            return;
        }

        try {

            if (!config.createFolderOutput()) {
                logger.warn("STRUCTURE FOLDER NOT CREATED, PLEASE CHECK IF ALREADY EXISTS");
                return;
            }

            logger.info("STAGE 1");
            logger.info("ANALYZING THE RESOURCES PATHS");

            ResourcesProviderAnalyzer resourcesProviderAnalyzer = new ResourcesProviderAnalyzer(config);
            SearchFileTraversal fileDiscover = new SearchFileTraversal(config)
                   .setSearchPaths(resourcesProviderAnalyzer.validateFilePaths(config));
            ArchModelConverter archModelConverter = new ArchModelConverter(config);

            fileDiscover.analyseModels(archModelConverter);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ERROR: " + e.getMessage());
        }
    }

}
