package org.discover.arch.model;

import com.opencsv.CSVWriter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.Config;
import org.eclipse.xtext.validation.Issue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osate.standalone.model.IRawModelLoader;
import org.osate.standalone.model.LoadAADLModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ArchModelConverter {

    Set<String> dataModelFiles = new HashSet<>();

    HashMap<String, Object> converterModelClassMap = new HashMap<>();

    JSONArray logsOutput = new JSONArray();

    List<OutputLoadedModelSchema> conversionOutput = new ArrayList<>();
    
    Config configObj;

    private final static Logger logger = LogManager.getLogger(ArchModelConverter.class);

    public ArchModelConverter(Config configObj) {
        // set config
        this.configObj = configObj;
    }

   
    void copyFoundedFiles() throws Exception {
        int id = 1;
        for (String pathFile : this.dataModelFiles) {
            String extension = SearchFileTraversal.getExtension(pathFile);
            File file = new File(pathFile);
            Path originalPath = Paths.get(pathFile);
            Path copied = Paths.get(Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), extension, id + "_" + file.getName()).toString());
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
            id++;
        }
    }


    private Map<String, Object> convertModelsUsingClass(String pathFile, String outPathXMI, String id) throws Exception {

        IRawModelLoader modelLoader = (IRawModelLoader) LoadAADLModel.getInstance();
        return modelLoader.loadModel(pathFile, outPathXMI, id, this.configObj);
    }

    void loggingConvertingResult() throws Exception {
        String jsonStr = this.logsOutput.toString(2); //TODO: This is impacting the heap memory
        try {
            FileWriter fw = new FileWriter(Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), "conversion-logs.json").toString());
            fw.write(jsonStr);
            fw.close();
        } catch (Exception e) {
            throw new Exception("error generating the logs .json " + e.getMessage());
        }
        try {
            var reports = this.generateReports();
            var jsonObject = new JSONObject(reports);
            jsonStr = jsonObject.toString(2);
            System.out.println(jsonObject);
            FileWriter fw = new FileWriter(Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), "reports-logs.json").toString());
            fw.write(jsonStr);
            fw.close();
        } catch (Exception e) {
            throw new Exception("error generating the reports .json" + e.getMessage());
        }

    }

    private JSONArray filterOutputResults(Iterable<Object> dataSource, Function<JSONObject, Boolean> fn) {
        JSONArray dataFiltered = new JSONArray();
        for (Object el : dataSource) {
            JSONObject item = (JSONObject) el;
            if (fn.apply(item))
                dataFiltered.put(item);
        }
        return dataFiltered;
    }

    private Map<String, HashMap<String, Object>> generateReports() {
        Map<String, HashMap<String, Object>> reports = new HashMap<>();
        for (String ext : this.configObj.getExtensionsForSearching()) {
            reports.put(ext, new HashMap<>());
            var filteredData = this.filterOutputResults(this.logsOutput, (JSONObject el) -> el.get("extension").equals(ext));
            reports.get(ext).put("totalFiles", filteredData.length());
            reports.get(ext).put("filesConverted", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isSavedTheModel").equals(true)).length());
            reports.get(ext).put("filesSuccessfullyParsed", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isParsingSucceeded").equals(true)).length());
            reports.get(ext).put("filesWithErrors", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isParsingSucceeded").equals(false)).length());
        }
        reports.put("general", new HashMap<>());
        reports.get("general").put("totalFilesDiscovered", this.logsOutput.length());
        reports.get("general").put("totalFilesSuccessfullyParsed", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isParsingSucceeded").equals(true)).length());

        reports.get("general").put("totalFilesConverted", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isSavedTheModel").equals(true)).length());

        reports.get("general").put("totalFilesWithErrors", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isParsingSucceeded").equals(false)).length());
        return reports;
    }

    void createCSVOfError() {
        try {
            File file = Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), "error-info.csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] header = {"model_name", "src_path", "is_parsed", "ref_resolving_error", "syntax_error", "error_codes"};
            writer.writeNext(header);
            for (OutputLoadedModelSchema element : this.conversionOutput) {
                String[] row = new String[header.length];
                row[0] = element.getModelName();
                row[1] = element.getPathAADLFile();
                row[2] = element.isParsingSucceeded() + "";
                int ref_resolving_error = 0;
                int syntaxError = 0;
                Set<String> errorCodes = new HashSet<>();
                for (Object error : element.getErrors(true)) {
                    if (error == null)
                        continue;
                    String errMessage = error.toString();
                    if (errMessage.contains("Couldn't resolve reference")) {
                        ref_resolving_error++;
                    }
                    if (error instanceof Issue) {
                        String code = ((Issue) error).getCode();
                        if (code != null)
                            errorCodes.add(code);
                        if (((Issue) error).isSyntaxError())
                            syntaxError++;
                    } else {
                        syntaxError++;
                    }
                }
                row[3] = ref_resolving_error + "";
                row[4] = syntaxError + "";
                row[5] = errorCodes.toString();
                writer.writeNext(row);
            }
            writer.close();
            System.out.println("GENERATED CSV OF ERROR REPORTS SUCCESSFULLY: " + file.getAbsolutePath());
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }
    }

    public void analyzeFileAndConvert(String pathToModelOrFolder) throws Exception {
        String outPathXMI = Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), "xmi").toAbsolutePath().toString() + "\\";

        // Ensure the directory exists before writing
        File mapFile = Paths.get("models", "map.csv").toFile();
        mapFile.getParentFile().mkdirs();

        File fileSrc = new File(pathToModelOrFolder);
        Map<String, Object> dataOutput = convertModelsUsingClass(pathToModelOrFolder, outPathXMI, fileSrc.getName());
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            List<String> aadlFiles = (List<String>) dataOutput.get(IRawModelLoader.MODEL_FILES_FOUND);
            List<String> docFiles = (List<String>) dataOutput.get(IRawModelLoader.DOC_FILES);
            this.dataModelFiles.addAll(aadlFiles);
            List<OutputLoadedModelSchema> dataOutputConversion = (List<OutputLoadedModelSchema>) dataOutput.get(IRawModelLoader.CONVERTING_OUTPUT);

            dataOutputConversion.forEach((OutputLoadedModelSchema out) -> {
                Map<String, Object> dataOutMap = out.toMap();
                dataOutMap.put("extension", "aadl");
                dataOutMap.put("docFiles", docFiles);
                this.conversionOutput.add(out);
                if (out.isParsingSucceeded()) {
                    try (FileWriter fw = new FileWriter(mapFile, true)) { // Append mode
                        logger.info("Path to model or folder: " + pathToModelOrFolder);
                        fw.write(pathToModelOrFolder + "," + out.getPathXMLFile() + "\n");
                    } catch (IOException e) {
                        logger.error("Error writing to map.txt: " + e.getMessage(), e);
                    }
                }
                this.logsOutput.put(new JSONObject(dataOutMap));
                // Save dataOutMap to a file
                saveDataOutMapToFile(dataOutMap, fileSrc.getName());
            });
            aadlFiles.forEach(this.configObj::putInCache);
        } finally {
            lock.unlock();
        }
    }

    private void saveDataOutMapToFile(Map<String, Object> dataOutMap, String modelName) {
        try {
            File file = Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolder(), "dataOutMaps", modelName + ".json").toFile();
            file.getParentFile().mkdirs(); // Create directories if they do not exist
            try (FileWriter fileWriter = new FileWriter(file)) {
                JSONObject json = new JSONObject(dataOutMap);
                fileWriter.write(json.toString(2));
            }
            System.out.println("Saved dataOutMap to file: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving dataOutMap to file: " + e.getMessage(), e);
        }
    }



}
