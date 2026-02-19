package org.osate.standalone.model;


import org.config.Config;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.List;
import java.util.Map;

public interface IRawModelLoader {
    Map<String, Object> loadModel(String pathAADLFile, String pathXMLFile, String id, Config configObj) throws Exception;

    List<Object> validateModel(Resource[] resources);

    String MODEL_FILES_FOUND = "modelFilesFound";
    String CONVERTING_OUTPUT = "convertingOutput";
    String DOC_FILES = "docFiles";

}
