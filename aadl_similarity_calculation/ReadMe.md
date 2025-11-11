# **AADL Architecture Similarity Calculation**

## ** Project Description**

This project implements a system for calculating the **similarity between software architectures** described using **AADL (Architecture Analysis & Design Language)**.  
The goal is to transform AADL models into **graph structures** and then compute two types of similarity between these graphs:

- **Structural Similarity**: Based on the structure of the graphs generated from the AADL models.  
- **Semantic Similarity**: Based on the names of the nodes in the graphs, which represent the various architectural components.

The system allows users to **weight the two similarity components** (structural and semantic), enabling customized balance in the final computation.

---

## ** Code Structure**

- **`Main.java`** – The main file that performs the conversion of models from XMI to JSON and computes similarities between the converted models.  
- **`Similarity.java`** – Contains methods for computing both structural and semantic similarities.  
- **`JsonConverter.java`** – Handles the conversion of AADL (XMI format) files into JSON files.  
- **`config.json`** – Configuration file that defines input/output directories and other execution parameters.

---

## ** How to Run**

### **Requirements**
- **Java 8+**  
- **Maven** (for dependency management)  
- **Apache Log4j** (for logging)

### **Configuration**

1. **Edit the `config.json` file:**
    - **`xmiFolderPath`**: Path to the folder containing XMI (AADL model) files.  
    - **`jsonFolderPath`**: Path to the folder where converted JSON files will be stored.  
    - **`structureSimilarityPath`**: Path to the CSV file for structural similarity output.  
    - **`semanticSimilarityPath`**: Path to the CSV file for semantic similarity output.  
    - **`averageSimilarityPath`**: Path to the CSV file for average similarity output.  
    - **`weight_of_structural`**: Weight assigned to structural similarity (between 0 and 1).  
    - **`weight_of_semantic`**: Weight assigned to semantic similarity (between 0 and 1).  
    - **`test`**: Boolean flag — set to `true` for test runs with multiple weights or `false` for a single run.  
    - **`datatype`**: `"AADL"` or `"Ecore"`.  
    - **`ecoreCSVFilePath`**: `"models/input/ecore_data.csv"`  

2. **Compile and Run:**
    ```bash
    mvn clean install
    java -jar target/project_name.jar
    ```

---

## ** Output**

The program generates three CSV files containing the calculated similarity matrices:

- **Structural Similarity**
- **Semantic Similarity**
- **Average Similarity**

Each matrix reports the similarity scores between every pair of models in the specified directory.

---

## ** Logging and Execution Time**

The program logs various informational and error messages using **Log4j**.  
Execution times for each process phase (conversion and similarity computation) are also recorded in the logs.

---

## ** Graph Format and Structure**

### **Introduction to Graphs**

A **graph** is a data structure that represents a collection of objects and their relationships.  
Objects are represented by **nodes (vertices)**, and relationships are represented by **edges (connections)**.  
Graphs are used to model complex systems, such as social networks, computer networks, and software architectures.

### **Graph Components**

1. **Nodes (Vertices)**  
   - Represent entities or objects in the graph, each with attributes such as:
     - **ID** – Unique identifier  
     - **Name** – Descriptive label  
     - **Category** – e.g., `process`, `thread`, `memory`, `device`  
     - **Type** – `software`, `hardware`, `system`, `feature`, or `unknown`  
   - In this project, nodes represent the **architectural components** described in AADL models.

2. **Connections (Edges)**  
   - Represent relationships between nodes. Each connection has a specific type:
     - **father** – Hierarchical parent-child relationship  
     - **feature** – Relationship between feature nodes  
     - **connection** – General connection between nodes  
   - Connections are **directed**, indicating the start and end of each relationship.

### **Graph Structure Example**

- **Nodes**: Represent architectural components such as processes, threads, devices, and memory.  
- **Connections**: Represent relationships between these components (e.g., a process communicating with a device).

### **Graph Construction Steps**

1. **Node Creation** – Nodes are created from JSON model data using attributes such as `name`, `category`, and `type`.  
2. **Connection Addition** – Edges are added between nodes to represent relationships defined in the model data.

### **Graph Visualization**

The system uses `JGraphXVisualization` to display the generated graphs, providing a **visual understanding** of the software architecture.

---

## **Graph Visualization**

To visualize the generated graphs, use the class **`JGraphXVisualization`** included in the project.  
It renders the graph in a graphical window using the **JGraphX** library.

---

## ** Graph Similarity Measurement**

### **Overview**

The class **`Similarity`**, located in the package `org.models.xmi.json.similarityMeasure`, is designed to compute the **similarity between two models represented as graphs** by comparing both their **structure** and **semantic content**.

### **Main Features**

- **Graph Construction** – Builds graph structures from JSON model files using `MakeCompleteGraph.buildGraphStructure`.  
- **Graph Comparison** – Computes structural and semantic similarity using `calculateComplete`, returning:
  1. Structural similarity score  
  2. Semantic similarity score  
  3. Weighted average similarity  

- **Maximum Common Subgraph (MCS)** – Identifies the largest common subgraph shared between two models.  
- **Graph Visualization** – Uses `JGraphXVisualization` to visualize the graphs and MCS for visual comparison.

---

### **Usage Example**

```java
JsonObject json1 = parseJsonFile(new File("path/to/first/model.json"));
JsonObject json2 = parseJsonFile(new File("path/to/second/model.json"));

double[] similarities = Similarity.calculateComplete(json1, json2, "Model1", "Model2", 0.5, 0.5);
System.out.println("Structural Similarity: " + similarities[0]);
System.out.println("Semantic Similarity: " + similarities[1]);
System.out.println("Average Similarity: " + similarities[2]);
