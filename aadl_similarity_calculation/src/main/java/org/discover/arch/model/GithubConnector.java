package org.discover.arch.model;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.Config;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.utils.Utils;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GithubConnector {

    private final static Logger logger = LogManager.getLogger(GithubConnector.class);

    private MetaData extractMetaData(String externalRepoURL) throws InvalidURLConnectorException {
        MetaData data = new MetaData();
        if (Utils.isValidPath(externalRepoURL)) {
            String[] parts = externalRepoURL.split("/");
            String name = parts[parts.length - 1];
            name = name.split("\\.")[0];
            data.name = name;
            if (!externalRepoURL.contains(".git")) {
                data.downloadablePath = externalRepoURL + ".git";
            }
        } else {
            throw new InvalidURLConnectorException();
        }
        return data;
    }

    public void loadResource(String externalRepoURL, String directoryPath, Config config) throws Exception {
        
        config.addPathToArchivesForSearching(directoryPath);
        MetaData metaData = this.extractMetaData(externalRepoURL);
        File clonedDirectory = Paths.get(directoryPath, metaData.name).toFile();
        Git git = null;
        try {
            if (!this.isReadyForDownload(clonedDirectory))
                return;
            
            this.deleteBeforeLoading(clonedDirectory.toString());
            logger.info("Start cloning repository: " + metaData.downloadablePath);

            git = Git.cloneRepository()
                    .setURI(metaData.downloadablePath)
                    .setDirectory(clonedDirectory)
                    .call();
            logger.info("End cloning repository: " + metaData.downloadablePath);

            config.putInCache(externalRepoURL);
            
        } catch (Exception e) {
            // if an error occours, remove directoryPath from Archives For Searching
            config.removePathFromArchivesForSearching(directoryPath);
            logger.error(e);
        } finally {
            if (git != null) {
                git.close();
                logger.info("Git connection closed");
            }
        }
    }

    public boolean isReadyForDownload(File repositoryDir) {

        logger.info("Analysing if there are new commits on: " + repositoryDir.toString());

        if (!repositoryDir.exists()) {
            logger.info("New commits found");
            return true;
        }

        Git git = null;

        try {
            git = Git.open(repositoryDir);
            git.fetch().call();
            ObjectId localHead = git.getRepository().resolve("HEAD");
            Collection<Ref> refs = git.lsRemote().setHeads(true).setTags(false).call();

            // Get the hash of the latest commit on the default branch
            String defaultBranchRef = "refs/heads/" + git.getRepository().getBranch();
            ObjectId remoteHead = refs.stream()
                    .filter(s -> s.toString().contains(defaultBranchRef))
                    .map(Ref::getObjectId)
                    .findFirst()
                    .orElse(null);

            if (localHead != null && localHead.equals(remoteHead)) {
                logger.info("New commits not found");
                return false;
            }
            logger.info("New commits found");
            return true;

        } catch (IOException | GitAPIException e) {
            logger.error(e);
            return false;
        } finally {
            if (git != null) {
                git.close();
                logger.info("Git connection closed");
            }
        }
    }

    public void deleteBeforeLoading(String clonedDirectoryRepo) throws Exception {
        File file = new File(clonedDirectoryRepo);
        if (!file.exists())
            return;
        Config.deleteDirectory(file.toPath());
    }
}
