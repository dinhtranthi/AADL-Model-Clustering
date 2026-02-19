package org.osate.standalone.model;

import com.google.inject.Injector;
import lombok.NoArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.Config;
import org.discover.arch.model.OutputLoadedModelSchema;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.instance.*;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.utils.Utils;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

@NoArgsConstructor
public class LoadAADLModel implements IRawModelLoader {
    private static LoadAADLModel INSTANCE = null;
    Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();;
    private final String PREDECLARED_PROPERTY_SET = Paths.get("aadl").toAbsolutePath().toString();

    private final static Logger logger = LogManager.getLogger(LoadAADLModel.class);

    public static LoadAADLModel getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        INSTANCE = new LoadAADLModel();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
        InstancePackage.eINSTANCE.eClass();
        return INSTANCE;
    }

    /**
     * Questo metodo carica un modello da file AADL, risolve i riferimenti incroci,
     * e restituisce un insieme di informazioni sull'esito del caricamento.
     *
     * @param pathAADLFileOrDirectory Percorso del file o della directory AADL da
     *                                caricare.
     * @param pathXMLFile             Percorso del file XML in cui salvare i file
     *                                XMI.
     * @param id                      Identificatore del modello.
     * @param configObj               Oggetto di configurazione.
     * @return Una mappa contenente informazioni sull'esito del caricamento del
     *         modello.
     * @throws Exception Lanciata in caso di errori durante il caricamento del
     *                   modello.
     */
    public Map<String, Object> loadModel(String pathAADLFileOrDirectory, String pathXMLFile, String id,
            Config configObj) throws Exception {
        // Inizializzazione di un set di risorse per l'utilizzo con Xtext.
        XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);

        // Risoluzione dei riferimenti incroci e ottenimento delle informazioni sui file
        // trovati.
        Map<String, Object> crossReferenceResolverOut = CrossReferenceResolver.resolveDown(pathAADLFileOrDirectory,
                configObj);
        List<String> pathToModelsFiles = (List<String>) crossReferenceResolverOut
                .get(CrossReferenceResolver.FOUND_FILES);
        List<String> pathToDocFiles = (List<String>) crossReferenceResolverOut.get(CrossReferenceResolver.DOC_FILES);
        List<OutputLoadedModelSchema> resultOutput = new ArrayList<>();
        Set<Resource> resourceSet;

        // Verifica l'esistenza dei file AADL e XML specificati.
        File fileAadl = new File(pathAADLFileOrDirectory);
        File fileXML = new File(pathXMLFile);
        if (!fileAadl.exists()) {
            throw new Exception("Il file AADL: " + pathAADLFileOrDirectory + " non esiste");
        }
        if (!fileXML.exists()) {
            throw new Exception("Il file per memorizzare i file XMI: " + pathXMLFile + " non esiste");
        }

        // Caricamento dei modelli AADL definiti.
        for (String modelPaths : pathToModelsFiles) {
            try {
                rs.getResource(URI.createFileURI(modelPaths), true);
            } catch (Exception e) {
                logger.error("LoadAADLModel@loadModel() -> " + e.getMessage());
            }
        }

        //////// CARICAMENTO DELLE DEFINIZIONI AADL PREDECLARE///////////
        Set<String> predeclaredFilesModelAADL = this.loadPredeclaredPropertySetsAADL(rs);
        /////////////////////////////////////////////////

        Set<String> resourcesInstantiated = new HashSet<>();
        for (Resource resource : rs.getResources()) {
            try {
                resource.load(null);
            } catch (Exception e) {
                logger.error("LoadAADLModel@loadModel(): errore nel caricamento delle risorse -> " + e.getMessage());
            }
        }

        resourceSet = new HashSet<>(rs.getResources());

        // Iterazione sulle risorse caricate.
        for (Resource resourceModel : resourceSet) {
            String resourceModelPath = resourceModel.getURI().toString();
            logger.info(" modello -> " + resourceModelPath);

            // Se il file è tra quelli predefiniti, si continua con il prossimo.
            if (predeclaredFilesModelAADL.contains(resourceModelPath)) {
                continue;
            }

            // Inizializzazione dello schema di output per il modello corrente.
            OutputLoadedModelSchema outputSchema = new OutputLoadedModelSchema();

            try {
                // Risoluzione di tutti i riferimenti incroci.
                EcoreUtil.resolveAll(resourceModel);

                // Impostazione dei parametri nello schema di output.
                outputSchema.setPathAADLFile(resourceModelPath);
                outputSchema.setPathXMLFile(pathXMLFile);
                outputSchema.setErrors(validateModel(new Resource[] { resourceModel }));

                // Controllo del contenuto della risorsa.
                List<EObject> contents = resourceModel.getContents();
                if (contents.size() == 0) {
                    throw new Exception("Questo modello: " + outputSchema.getPathAADLFile()
                            + " non può essere caricato, potrebbe essere corrotto");
                }

                // Verifica se il contenuto è di tipo AadlPackage.
                if (!(contents.get(0) instanceof AadlPackage)) {
                    throw new Exception("Questo modello: " + outputSchema.getPathAADLFile() + " non è un AadlPackage");
                }

                // Ottenimento dell'AadlPackage.
                AadlPackage aadlPackage = (AadlPackage) contents.get(0);
                outputSchema.setModelName(aadlPackage.getFullName());

                // Ottenimento delle implementazioni di System.
                List<SystemImplementation> systemImplementations = aadlPackage.getPublicSection().getOwnedClassifiers()
                        .stream()
                        .filter((Classifier classifier) -> classifier instanceof SystemImplementationImpl)
                        .map((Classifier el) -> (SystemImplementation) el).toList();

                // Iterazione sulle implementazioni di System.
                for (SystemImplementation systemImpl : systemImplementations) {
                    OutputLoadedModelSchema output = new OutputLoadedModelSchema(outputSchema);
                    SystemInstance systemInstance;
                    try {
                        // Controllo dell'effettiva validità di un modello, se non ha nomi di
                        // componenti/connettori/feature/flowspec di lunghezza almeno 3 non lo trasformo
                        // sarebbe poco utile e sporcherebbe il dataset di modelli utili
                        // Istanza del modello.
                        systemInstance = InstantiateModel.instantiate(systemImpl);
                        // Flag di controllo
                        boolean spouriousModel = true;
                        EList<ComponentInstance> components = systemInstance.getAllComponentInstances();
                        EList<ConnectionInstance> connections = systemInstance.getAllConnectionInstances();
                        EList<FeatureInstance> features = systemInstance.getAllFeatureInstances();
                        EList<FlowSpecificationInstance> flowSpecifications = systemInstance.getFlowSpecifications();
                        int validNameMinimumLength = configObj.getValidNameMinimumLength();
                        
                        
                        int i = 0;
                        // check 
                        while (spouriousModel && i < components.size()) {
                            ComponentInstance component = components.get(i);
                            String componentName = Utils.replaceStringIgnoreCase(component.getFullName(),configObj.getCommon_words_to_exclude(), "");
                            if (componentName.length() > validNameMinimumLength) {
                                // Fai qualcosa con l'oggetto Connection x
                                spouriousModel = false;
                            }
                            i++;
                        }
                        i = 0;
                        while (spouriousModel && i < connections.size()) {
                            ConnectionInstance connection = connections.get(i);
                            if (connection.getFullName().length() > 2) {
                                // Fai qualcosa con l'oggetto Connection x
                                spouriousModel = false;
                            }
                            i++;
                        }
                        i=0;
                        while (spouriousModel && i < features.size()) {
                            FeatureInstance feature = features.get(i);
                            String featureName = Utils.replaceStringIgnoreCase(feature.getFullName(),configObj.getCommon_words_to_exclude(), "");
                            if (featureName.length() > validNameMinimumLength) {
                                // Fai qualcosa con l'oggetto Connection x
                                spouriousModel = false;
                            }
                            i++;
                        }
                        i= 0;
                        while (spouriousModel && i < flowSpecifications.size()) {
                            FlowSpecificationInstance flowSpecification = flowSpecifications.get(i);
                            String flowSpecificationName = Utils.replaceStringIgnoreCase(flowSpecification.getFullName(),configObj.getCommon_words_to_exclude(), "");
                            if (flowSpecificationName.length() > validNameMinimumLength) {
                                // Fai qualcosa con l'oggetto Connection x
                                spouriousModel = false;
                            }
                            i++;
                        }

                        if (spouriousModel) {
                            output.setSavedTheModel(false);
                            //
                            outputSchema.getErrors().add(
                                    "Il modello non possiede elementi con nomi di lunghezza superiore a 2, viene quindi considerato invalido perchè non utile al fine dell'analisi");
                            outputSchema.setParsingSucceeded(false);
                            logger.info(
                                    "Il modello non possiede elementi con nomi di lunghezza superiore a 2, viene quindi considerato invalido perchè non utile al fine dell'analisi");
                            resultOutput.add(outputSchema);
                        } else {
                            output.setSavedTheModel(true);
                            output.setPathXMLFile(
                                    saveModelToXMI(systemInstance, rs, pathXMLFile, output.getModelName(), id,
                                            resourcesInstantiated));
                            resultOutput.add(output);
                        }

                    } catch (final Exception e) {
                        output.getErrors().add(e.getMessage());
                        output.setSavedTheModel(false);
                        output.setParsingSucceeded(false);
                        resultOutput.add(output);

                        logger.error("Errore nell'istanziazione del modello: " + output.getModelName() +
                                " la cui istanza del sistema è: " + systemImpl.getName() + ": " + e);
                    }
                }

            } catch (Exception e) {
                outputSchema.getErrors().add(e.getMessage());
                outputSchema.setParsingSucceeded(false);
                logger.error("Errore: " + e);
                resultOutput.add(outputSchema);
            }
        }

        // Pulizia delle risorse non più necessarie.
        rs = null;
        resourceSet = null;
        resourcesInstantiated = null;

        // Creazione della mappa di output.
        Map<String, Object> dataOutput = new HashMap<>();
        dataOutput.put(MODEL_FILES_FOUND, pathToModelsFiles);
        dataOutput.put(CONVERTING_OUTPUT, resultOutput);
        dataOutput.put(DOC_FILES, pathToDocFiles);
        return dataOutput;
    }

    public List<Object> validateModel(Resource[] resources) {
        List<Issue> issues = new ArrayList<>();
        for (final Resource resource : resources) {
            IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
                    .getResourceValidator();
            try {
                issues = validator.validate(resource, CheckMode.NORMAL_AND_FAST, CancelIndicator.NullImpl);
            } catch (Exception e) {
                System.err.println("****************************** " + e);
            }
        }
        List<Object> error = new ArrayList<>(issues);
        return error.subList(0, Math.min(10, error.size()));
    }

    private String saveModelToXMI(SystemInstance systemInstance, XtextResourceSet rs, String pathXMLFile,
            String parentName, String id, Set<String> resourcesInstantiated) throws Exception {

        String instanceName = pathXMLFile;
        if (id != null) {
            instanceName += id + "_";
        }
        if (parentName != null) {
            instanceName += parentName + "_";
        }

        String simple_name = systemInstance.getName().replaceAll("_Instance", "");
        int indexName = 1;
        instanceName += simple_name + "_" + indexName;

        while (resourcesInstantiated.contains(instanceName)) {
            List<String> name_split = new ArrayList<>(List.of(instanceName.split("_")));
            name_split.remove(name_split.size() - 1);
            instanceName = String.join("_", name_split) + "_" + indexName;
            indexName++;
        }
        resourcesInstantiated.add(instanceName);
        instanceName += ".aaxl2";

        Resource xmiResource = rs.createResource(URI.createFileURI(instanceName));
        xmiResource.getContents().add(systemInstance);
        xmiResource.save(null);
        xmiResource = null;
        return instanceName;
    }

    /**
     * 
     * @param rs
     * @return
     */
    private Set<String> loadPredeclaredPropertySetsAADL(XtextResourceSet rs) {
        Set<String> predeclaredFilesModelAADL = new HashSet<>();
        File file = new File(this.PREDECLARED_PROPERTY_SET);
        for (File fileChild : Objects.requireNonNullElse(file.listFiles(), new File[0])) {
            String pathTo_AADL_Resource = fileChild.getAbsolutePath();
            Resource resource = rs.getResource(URI.createFileURI(pathTo_AADL_Resource), true);
            predeclaredFilesModelAADL.add(resource.getURI().toString());
        }
        return predeclaredFilesModelAADL;
    }

    

}
