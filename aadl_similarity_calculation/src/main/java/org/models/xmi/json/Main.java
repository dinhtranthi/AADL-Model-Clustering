package org.models.xmi.json;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.models.xmi.json.similarityMeasure.Similarity;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Davide Soldati
 */
public class Main {
    private final static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        JsonObject config = loadConfiguration("src/main/resources/config_similarity_measure.json");
        String xmiFolderPath = config.getString("xmiFolderPath");
        String jsonFolderPath = config.getString("jsonFolderPath");
        String structureSimilarityPath = config.getString("structureSimilarityPath");
        String semanticSimilarityPath = config.getString("semanticSimilarityPath");
        String averageSimilarityPath = config.getString("averageSimilarityPath");

        String[] paths = {
                config.getString("xmiFolderPath"),
                config.getString("jsonFolderPath"),
                config.getString("structureSimilarityPath"),
                config.getString("semanticSimilarityPath"),
                config.getString("averageSimilarityPath")
        };


        // This causes errors when the program tries to create directories on paths like output/max_sim_struct.csv
        // for (String path : paths) {
        //     File file = new File(path);
        //     if (!file.exists()) {
        //         if (file.mkdirs()) {
        //             logger.info("Directory created: " + path);
        //         } else {
        //             logger.error("Failed to create directory: " + path);
        //         }
        //     }
        // }


        // Create files and folders
        for (String path : paths) {
            File file = new File(path);
            // If it is a file (with extension)
            if (path.matches(".*\\.[a-zA-Z0-9]+$")) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (parentDir.mkdirs()) {
                        logger.info("Parent directory created: " + parentDir.getPath());
                    } else {
                        logger.error("Failed to create parent directory: " + parentDir.getPath());
                        continue;
                    }
                }
                try {
                    // Check and create file if it does not exist
                    if (file.createNewFile()) {
                        logger.info("File created: " + file.getPath());
                    } else {
                        logger.debug("File already exists: " + file.getPath());
                    }
                } catch (IOException e) {
                    logger.error("Failed to create file: " + file.getPath(), e);
                }
                continue;
            }

            // If it is a directory, check and create
            if (!file.exists()) {
                if (file.mkdirs()) {
                    logger.info("Directory created: " + path);
                } else {
                    logger.error("Failed to create directory: " + path);
                }
            }
        }

        double weight_structural = config.getJsonNumber("weight_of_structural").doubleValue();
        double weight_semantic = config.getJsonNumber("weight_of_semantic").doubleValue();
        boolean test = config.getBoolean("test");

        if (weight_structural + weight_semantic != 1 && !test) {
            logger.error("The sum of the weights must be equal to 1");
            return;
        }
   
/***************************************************************************************************
 ******************* Start Convert XMI to JSON  ******************************************************
 ***************************************************************************************************/
   

        File xmiFolder = new File(xmiFolderPath);
        if (!xmiFolder.exists()) {
            logger.error("XMI folder does not exist: " + xmiFolderPath);
            return;
        }
        logger.info("Checking XMI folder path: " + xmiFolder.getAbsolutePath());

        File jsonFolder = new File(jsonFolderPath);
        //delete all files in the folder
        if (jsonFolder.exists()) {
            File[] files = jsonFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        logger.error("Failed to delete file: " + file.getName());
                    }
                }
            }
        }
        if (!jsonFolder.exists()) {
            if (jsonFolder.mkdirs()) {
                logger.info("JSON folder created: " + jsonFolderPath);
            } else {
                logger.error("Failed to create JSON folder: " + jsonFolderPath);
                return;
            }
        }

        JsonConverter.ConvertToJson(xmiFolderPath, jsonFolderPath);
        long endTime = System.currentTimeMillis();
        logger.info("Time taken to convert XMI to JSON: " + (endTime - startTime) + " ms");

/***************************************************************************************************
 ******************* Convert XMI to JSON DONE ******************************************************
 ***************************************************************************************************/


        logger.info("Starting similarity calculation...");
        logger.info("////////////////////////////////////////////////////////////////////////////////////////");

        long similarityStartTime = System.currentTimeMillis();
        List<File> files = listFiles(jsonFolderPath);

        int numFiles = files.size();

        System.out.println("Number of files: " + numFiles);
        logger.info("Number of files: " + numFiles);

        double[][] graphSimilarityMatrix = new double[numFiles][numFiles];
        double[][] semanticSimilarityMatrix = new double[numFiles][numFiles];


        double[][] averageSimilarityMatrix = new double[numFiles][numFiles];



        Set<String> compared = new HashSet<>();

        if (test) {
            for (int i = 0; i <= 10; i++) {
                double wStruct = i * 0.1;
                double wSemantic = 1.0 - wStruct;
                wStruct = Math.round(wStruct * 10.0) / 10.0;
                String structurePath = structureSimilarityPath.replace(".csv", "_wStruct_" + wStruct + ".csv");
                String semanticPath = semanticSimilarityPath.replace(".csv", "_wStruct_" + wStruct + ".csv");
                String averagePath = averageSimilarityPath.replace(".csv", "_wStruct_" + wStruct + ".csv");

                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<?>> futures = calculateSimilarities(files, graphSimilarityMatrix, semanticSimilarityMatrix, averageSimilarityMatrix, wStruct, wSemantic, executor);
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                // Lista per i nomi dei file per la riga e la colonna del CSV
                List<String> modelNames = new ArrayList<>();
                for (File file : files) {
                    modelNames.add(file.getName());
                }
                saveMatrixToCSV(averageSimilarityMatrix, averagePath, modelNames);
            }
        } else {
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            List<Future<?>> futures = calculateSimilarities(files, graphSimilarityMatrix, semanticSimilarityMatrix, averageSimilarityMatrix, weight_structural, weight_semantic, executor);
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Lista per i nomi dei file per la riga e la colonna del CSV
            List<String> modelNames = new ArrayList<>();
            for (File file : files) {
                modelNames.add(file.getName());
            }

            // Metodo per salvare matrici in CSV
            saveMatrixToCSV(graphSimilarityMatrix, structureSimilarityPath, modelNames);
            saveMatrixToCSV(semanticSimilarityMatrix, semanticSimilarityPath, modelNames);
            saveMatrixToCSV(averageSimilarityMatrix, averageSimilarityPath, modelNames);
        }

        long similarityEndTime = System.currentTimeMillis();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(similarityEndTime - similarityStartTime);
        logger.info("Time taken for similarity calculations: " + minutes + " minutes");
    }

    private static List<Future<?>> calculateSimilarities(List<File> files, double[][] graphSimilarityMatrix, double[][] semanticSimilarityMatrix, double[][] averageSimilarityMatrix, double w_struct, double w_semantic, ExecutorService executor) throws IOException {
        HashSet<String> compared = new HashSet<>();
        JsonObject config = loadConfiguration("src/main/resources/config_similarity_measure.json");
        boolean test = config.getBoolean("test");
        System.out.println("Calculating similarities..." + w_struct + " " + w_semantic);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            for (int j = i; j < files.size(); j++) {
                File file1 = files.get(i);
                File file2 = files.get(j);

                final int index1 = i;
                final int index2 = j;

                Future<?> future = executor.submit(() -> {
                    try {
                        JsonObject json1 = parseJsonFile(file1);
                        JsonObject json2 = parseJsonFile(file2);
                        String comparisonKey1 = file1.getName() + file2.getName();
                        String comparisonKey2 = file2.getName() + file1.getName();

                        if (!compared.contains(comparisonKey1) && !compared.contains(comparisonKey2)) {
                            logger.info("Comparing " + file1.getName() + " and " + file2.getName());
                            double[] similarities = Similarity.calculateComplete(json1, json2, file1.getName(), file2.getName(), w_struct, w_semantic);

                            System.out.println("Structural Similarity: " + similarities[0]);
                            System.out.println("Semantic Similarity: " + similarities[1]);
                            System.out.println("Average Similarity: " + similarities[2]);            

                            if (!test) {
                                graphSimilarityMatrix[index1][index2] = similarities[0];
                                graphSimilarityMatrix[index2][index1] = similarities[0];
                                semanticSimilarityMatrix[index1][index2] = similarities[1];
                                semanticSimilarityMatrix[index2][index1] = similarities[1];
                            }
                            averageSimilarityMatrix[index1][index2] = similarities[2];
                            averageSimilarityMatrix[index2][index1] = similarities[2];
                            compared.add(comparisonKey1);
                            compared.add(comparisonKey2);

                            logger.info("Comparison done.");
                        } else {
                            logger.info("Comparison already done: " + file1.getName() + " and " + file2.getName());
                        }
                    } catch (IOException e) {
                        logger.error("Error reading files: " + file1.getName() + " or " + file2.getName(), e);
                    }
                });
                futures.add(future);
            }
        }
        return futures;
    }

    // Method to load configuration from a JSON file
    public static JsonObject loadConfiguration(String configFilePath) throws IOException {
        try (FileReader fileReader = new FileReader(configFilePath);
             JsonReader jsonReader = Json.createReader(fileReader)) {
            return jsonReader.readObject();
        } catch (IOException e) {
            logger.error("Error reading configuration file: " + configFilePath, e);
            throw e;
        }
    }

    // Method to list all files in a directory
    public static List<File> listFiles(String directoryPath) {
        List<File> fileList = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    fileList.add(file);
                }
            }
        } else {
            logger.warn("No files found in the directory: " + directoryPath);
        }
        return fileList;
    }

    // Method to read and parse a JSON file
    public static JsonObject parseJsonFile(File file) throws IOException {
        try (FileReader fileReader = new FileReader(file);
             JsonReader jsonReader = Json.createReader(fileReader)) {
            return jsonReader.readObject();
        } catch (IOException e) {
            logger.error("Error reading JSON file: " + file.getName(), e);
            throw e;
        }
    }

    // Method to save a matrix to a CSV file
    public static void saveMatrixToCSV(double[][] matrix, String filePath, List<String> modelNames) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write the header
            writer.append("Model");
            for (String modelName : modelNames) {
                writer.append(",").append(modelName);
            }
            writer.append("\n");

            // Write the data
            for (int i = 0; i < matrix.length; i++) {
                writer.append(modelNames.get(i));
                for (int j = 0; j < matrix[i].length; j++) {
                    writer.append(",").append(String.valueOf(matrix[i][j]));
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            logger.error("Error writing CSV file: " + filePath, e);
            throw e;
        }
    }
}
