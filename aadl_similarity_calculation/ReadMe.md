# **Calcolo della Similarità di Architetture AADL**

## **Descrizione del Progetto**

Questo progetto implementa un sistema per calcolare la similarità tra architetture software descritte tramite AADL (Architecture Analysis & Design Language). L'obiettivo è trasformare i modelli AADL in grafi e poi calcolare due tipi di similarità tra questi grafi:

- **Similarità Strutturale**: Basata sulla struttura dei grafi generati dai modelli AADL.
- **Similarità Semantica**: Basata sui nomi dei nodi nei grafi, che rappresentano i vari componenti architetturali.

Il sistema permette di pesare le due componenti della similarità (strutturale e semantica) in modo che l'utente possa bilanciare il calcolo secondo le proprie necessità.

## **Struttura del Codice**

- **`Main.java`**: È il file principale che esegue il processo di conversione dei modelli da XMI a JSON, e poi calcola le similarità tra i modelli convertiti.
- **`Similarity.java`**: Contiene i metodi per il calcolo delle similarità strutturale e semantica.
- **`JsonConverter.java`**: Gestisce la conversione dei file AADL (in formato XMI) in file JSON.
- **`config.json`**: File di configurazione che definisce i percorsi delle directory di input/output e altri parametri necessari per l'esecuzione del programma.

## **Istruzioni per l'Esecuzione**

### **Requisiti**

- **Java 8+**
- **Maven** (per la gestione delle dipendenze)
- **Apache Log4j** (per il logging)

### **Configurazione**

1. **Modifica il file `config.json`**:
    - **`xmiFolderPath`**: Percorso della cartella contenente i file XMI (modelli AADL).
    - **`jsonFolderPath`**: Percorso della cartella dove verranno salvati i file JSON convertiti.
    - **`structureSimilarityPath`**: Percorso del file CSV dove verrà salvata la similarità strutturale.
    - **`semanticSimilarityPath`**: Percorso del file CSV dove verrà salvata la similarità semantica.
    - **`averageSimilarityPath`**: Percorso del file CSV dove verrà salvata la similarità media.
    - **`weight_of_structural`**: Peso assegnato alla similarità strutturale (tra 0 e 1).
    - **`weight_of_semantic`**: Peso assegnato alla similarità semantica (tra 0 e 1).
    - **`test`**: Booleano che determina se eseguire calcoli di test con diversi pesi (`true`) o una singola esecuzione (`false`).
    - **`datatype`**:: "AADL" or "Ecore".
    - **`ecoreCSVFilePath`**: "models/input/ecore_data.csv"

2. **Compila ed esegui il programma**:
    - Apri un terminale nella directory del progetto e usa Maven per compilare il codice:
      ```bash
      mvn clean install
      ```
    - Esegui il programma:
      ```bash
      java -jar target/nome_del_progetto.jar
      ```

### **Output**

Il programma genererà tre file CSV contenenti le matrici di similarità calcolate:

- **Similarità Strutturale**
- **Similarità Semantica**
- **Similarità Media**

Ogni matrice riporta le similarità tra ogni coppia di modelli presenti nella cartella specificata.

## **Log e Tempi di Esecuzione**

Il programma registra vari messaggi informativi e di errore tramite Log4j. I tempi di esecuzione di ciascuna fase del processo (conversione e calcolo delle similarità) vengono riportati nel log.
## **Formato e Struttura di un Grafo**

### **Introduzione ai Grafi**

Un grafo è una struttura dati che rappresenta un insieme di oggetti e le relazioni tra di essi. Nei grafi, gli oggetti sono rappresentati dai **nodi** e le relazioni tra di essi sono rappresentate dalle **connessioni** o **archi**. I grafi sono utilizzati per modellare sistemi complessi, come reti sociali, reti di computer e architetture software.

### **Componenti di un Grafo**

1. **Nodi (o Vertici)**
    - I **nodi** rappresentano gli oggetti o le entità nel grafo. Ogni nodo può avere vari attributi, come:
        - **ID**: Un identificatore unico per il nodo.
        - **Nome**: Una stringa che descrive il nodo.
        - **Categoria**: La categoria del nodo (ad esempio, `process`, `thread`, `memory`, `device`).
        - **Tipo**: Il tipo del nodo, che può essere `software`, `hardware`, `system`, `feature`, o `unknown`.
    - Nel contesto del nostro progetto, i nodi rappresentano i componenti architetturali descritti nei modelli AADL.

2. **Connessioni (o Archi)**
    - Le **connessioni** rappresentano le relazioni tra i nodi nel grafo. Ogni connessione può avere un tipo specifico, come:
        - **father**: Indica una relazione di tipo gerarchico tra un nodo genitore e un nodo figlio.
        - **feature**: Indica una relazione tra nodi di tipo caratteristica o funzionalità.
        - **connection**: Rappresenta una connessione generale tra due nodi.
    - Le connessioni sono dirette, ovvero hanno una direzione che indica la partenza e l'arrivo della relazione.

### **Esempio di Struttura di un Grafo**

Nel nostro progetto, un grafo può essere strutturato come segue:

- **Nodi**: Rappresentano componenti architetturali come processi, thread, dispositivi e memoria.
- **Connessioni**: Rappresentano le relazioni tra questi componenti, ad esempio, un processo che comunica con un dispositivo o un thread che è gestito da un processo.

### **Costruzione del Grafo**

La costruzione del grafo avviene attraverso i seguenti passi:

1. **Creazione dei Nodi**: I nodi sono creati a partire dai dati del modello JSON, utilizzando attributi come `name`, `category`, e `type`.
2. **Aggiunta delle Connessioni**: Le connessioni vengono aggiunte tra i nodi per rappresentare le relazioni definite nei dati del modello. Ogni connessione è caratterizzata da un tipo specifico che determina la natura della relazione.

### **Visualizzazione del Grafo**

Per visualizzare il grafo, utilizziamo strumenti come `JGraphXVisualization` che permettono di generare una rappresentazione grafica del grafo, facilitando l'analisi visiva delle architetture software.

## **Visualizzazione dei Grafi**

Per visualizzare i grafi generati dal sistema, puoi utilizzare la classe `JGraphXVisualization` inclusa nel progetto. Questa classe permette di visualizzare il grafo in una finestra grafica utilizzando la libreria `JGraphX`.
## Misurazione della Similarità dei Grafi

### Panoramica

La classe `Similarity`, situata nel pacchetto `org.models.xmi.json.similarityMeasure`, è progettata per calcolare la similarità tra due modelli rappresentati come grafi. Questo viene realizzato confrontando la struttura e il contenuto semantico dei grafi.

### Funzionalità

La classe `Similarity` fornisce diverse funzionalità chiave:

- **Costruzione dei Grafi**: Costruisce strutture di grafo a partire da file JSON che rappresentano diversi modelli. I grafi vengono costruiti utilizzando il metodo `MakeCompleteGraph.buildGraphStructure`.

- **Confronto dei Grafi**: La classe confronta due grafi utilizzando misure di similarità strutturale e semantica. Questo confronto è eseguito dal metodo `calculateComplete`, che restituisce un array contenente:
   1. Il punteggio di similarità strutturale.
   2. Il punteggio di similarità semantica.
   3. La similarità media, calcolata come combinazione ponderata delle due precedenti.

- **Sottografo Massimo Comune**: La classe è in grado di identificare il Sottografo Massimo Comune (MCS) tra due grafi, che rappresenta la parte comune massima tra i due modelli in termini di nodi e connessioni.

- **Visualizzazione dei Grafi**: Utilizzando la classe `JGraphXVisualization`, è possibile visualizzare i grafi generati e il MCS per facilitare l'analisi visiva delle somiglianze tra i modelli.

### Esempio d'Uso

Ecco un esempio di come utilizzare la classe `Similarity`:

```java
JsonObject json1 = parseJsonFile(new File("path/to/first/model.json"));
JsonObject json2 = parseJsonFile(new File("path/to/second/model.json"));

double[] similarities = Similarity.calculateComplete(json1, json2, "Model1", "Model2", 0.5, 0.5);
System.out.println("Similarità Strutturale: " + similarities[0]);
System.out.println("Similarità Semantica: " + similarities[1]);
System.out.println("Similarità Media: " + similarities[2]);
```
Questo esempio mostra come caricare due modelli JSON, confrontarli e stampare i punteggi di similarità strutturale, semantica e media.

### **Uso di JGraphXVisualization**

Per visualizzare un grafo, utilizza il metodo `VisualizeGraph` fornendo come argomento il grafo da visualizzare e il nome del file per il salvataggio (se necessario). Il grafo sarà visualizzato in una finestra separata, permettendo di esplorare visivamente la struttura dell'architettura.

Esempio:
```java
Graph<Node, Connection> myGraph = // ... inizializzazione del grafo
JGraphXVisualization.VisualizeGraph(myGraph, "output_graph.png");
```

## **Autore**

- **Davide Soldati**
