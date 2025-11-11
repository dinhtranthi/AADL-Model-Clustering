package org.discover.arch.model;

import java.util.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OutputLoadedModelSchema {
    private List<Object> errors;
    private String pathXMLFile;
    private String pathAADLFile;
    private String modelName;
    private boolean isParsingSucceeded = true;
    private boolean isSavedTheModel = false;


    public OutputLoadedModelSchema(OutputLoadedModelSchema o) {
        setErrors(o.getErrors());
        setPathAADLFile(o.getPathAADLFile());
        setModelName(o.getModelName());
        setPathXMLFile("");
        setParsingSucceeded(true); 
        setSavedTheModel(false);
    }
    

    public List<Object> getErrors(boolean filtered) {
        if (this.errors == null)
            return new ArrayList<>();
        return this.errors.stream().filter((Object x) -> {
            if (!filtered || x == null)
                return true;
            String msg = x.toString();
            return !msg.contains("Error executing EValidator");
        }).toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        List<String> listErrors = this.getErrors(true).stream().map((x) -> {
            try {
                return x.toString();
            } catch (Exception e) {
                return "Error";
            }
        }).toList();
        listErrors = listErrors.stream().map((String e) -> {
            if (e.length() > 255)
                return e.substring(0, 255) + "...";
            return e;
        }).toList();
        data.put("modelName", this.modelName);
        data.put("isParsingSucceeded", this.isParsingSucceeded);
        data.put("isSavedTheModel", this.isSavedTheModel);
        data.put("pathAADLFile", this.pathAADLFile);
        data.put("pathXMLFile", this.pathXMLFile);
        data.put("errors", listErrors);
        return data;
    }
}
