package org.discover.arch.model;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.Config;
import org.utils.Utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourcesProviderAnalyzer {

    private GithubConnector githubConnector = new GithubConnector();

    private final static Logger logger = LogManager.getLogger(ResourcesProviderAnalyzer.class);

    public ResourcesProviderAnalyzer(Config config) {

        this.validateExternalPaths(config);

        config.persistCacheInDisk();
    }

    public List<String> validateFilePaths(Config config) {
        List<String> validFilesPaths = new ArrayList<>();
        for (String p : config.getArchivesForSearching()) {
            File file = new File(p);
            if (file.exists())
                validFilesPaths.add(p);
        }
        if (validFilesPaths.size() == 0) {
            logger.warn("THERE IS NOT VALID PATH IN THE LIST PROVIDED, PLEASE VERIFY YOUR CONFIGURATION");
        }
        return validFilesPaths;
    }

    private void validateExternalPaths(Config config) {
        logger.info("ANALYZING THE EXTERNAL PATHS THIS MAY TAKE A BIT LONGER, DEPENDS OF THE CACHE TIME CONFIGURATION");
        int delayCache = config.getTimeCacheForPollingFromExternalResources();

        long startTime = System.nanoTime();

        for (String path : config.getExternalResources()) {
            if (!Utils.isValidPath(path))
                logger.warn("Not found connector to external result : " + path);
            else {
                String directoryPath = Paths.get(config.getRootPath(), "github").toAbsolutePath().toString();
                // Layer of validation that checks for the config expiration times
                if (config.isInCache(path, delayCache)) {
                    continue;
                }
                try {
                    githubConnector.loadResource(path, directoryPath, config);
                } catch (Exception e) {
                    logger.error("Error analysing the external resource: " + path);
                    e.printStackTrace();
                }
            }

        }
        long endTime = System.nanoTime();
        logger.info("Validation external path execution time in seconds: "
                + ((endTime - startTime) / 1000000000) + " s");

    }

}
