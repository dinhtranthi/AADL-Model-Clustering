package org.models.xmi.json.similarityMeasure;

import com.github.jfasttext.JFastText;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class SemanticSimilarityFastText {

    private static final Logger logger = LogManager.getLogger(SemanticSimilarityFastText.class);

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "_", "this", "instance", "impl", "imp", "sensor", "subsystem", "system",
            "sys", "sub", "single", "dual", "integration", "standard", "with",
            "functional", "devices", "hardware"
    ));

    private static final JFastText FAST_TEXT = new JFastText();

    static {
        // Load the pre-trained FastText model
        FAST_TEXT.loadModel("models/cc.en.300.bin");
    }


    public static void main(String[] args) {
        String inputFilePath = "src/main/java/org/models/xmi/json/map_tab(Ripristinato automaticamente).CSV";
        String outputFilePath = "models/semantic_results.csv";

        List<List<String>> listOfWordLists = loadWordListsFromCSV(inputFilePath);
        if (listOfWordLists.isEmpty()) {
            System.err.println("No data found in the input file.");
            return;
        }

        try {
            calculateAndSaveSimilarities(listOfWordLists, outputFilePath);
        } catch (IOException e) {
            System.err.println("Error writing results to CSV: " + e.getMessage());
        }
    }

    private static List<List<String>> loadWordListsFromCSV(String filePath) {
        List<List<String>> wordLists = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Split and clean the words in each line
                List<String> words = Arrays.stream(line.split(";")[1].split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                wordLists.add(words);
            }
        } catch (IOException e) {
            System.err.println("Error reading from CSV file: " + e.getMessage());
        }

        return wordLists;
    }

    private static void calculateAndSaveSimilarities(List<List<String>> wordLists, String outputFilePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            writer.println("List1,List2,Similarity");

            for (int i = 0; i < wordLists.size(); i++) {
                for (int j = i + 1; j < wordLists.size(); j++) {
                    double similarity = calculateSemanticSimilarity(wordLists.get(i), wordLists.get(j));
                    writer.printf("\"%s\",\"%s\",\"%.4f\"%n", wordLists.get(i), wordLists.get(j), similarity);
                }
            }
        }
    }

    public static double calculateSemanticSimilarity(List<String> list1, List<String> list2) {

        if (FAST_TEXT == null) {
            logger.error("FastText model has not been loaded.");
            return 0.0;
        }

        List<String> cleanedList1 = cleanList(list1);
        List<String> cleanedList2 = cleanList(list2);

        Optional<double[]> vec1 = calculateAverageVector(cleanList(list1));
        Optional<double[]> vec2 = calculateAverageVector(cleanList(list2));

        if (vec1.isEmpty()) {
            System.out.println("Vector for List1 is empty.");
        } 

        if (vec2.isEmpty()) {
            System.out.println("Vector for List2 is empty.");
        } 

        return vec1.flatMap(v1 -> vec2.map(v2 -> cosineSimilarity(v1, v2)))
                .orElse(0.0);
    }

    private static List<String> cleanList(List<String> list) {
        return list.stream()
                .map(SemanticSimilarityFastText::removeKeywords)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String removeKeywords(String str) {
        String cleanStr = str.replaceAll("[0-9]", "")
                .replaceAll("[()]", "")
                .replaceAll("_", " ")
                .toLowerCase();

        for (String keyword : KEYWORDS) {
            cleanStr = cleanStr.replaceAll("\\b" + keyword + "\\b", "").trim();
        }

        return cleanStr.length() > 2 ? cleanStr : "";
    }

    private static Optional<double[]> calculateAverageVector(List<String> list) {
        int vectorSize = FAST_TEXT.getVector("word").size();
        double[] averageVector = new double[vectorSize];
        int validWordCount = 0;

        for (String word : list) {
            List<Float> wordVector = FAST_TEXT.getVector(word);
            if (wordVector != null && !wordVector.isEmpty()) {
                validWordCount++;
                for (int i = 0; i < vectorSize; i++) {
                    averageVector[i] += wordVector.get(i);
                }
            }
        }

        if (validWordCount == 0) {
            return Optional.empty();
        }

        for (int i = 0; i < vectorSize; i++) {
            averageVector[i] /= validWordCount;
        }

        return Optional.of(averageVector);
    }

    private static double cosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        return (norm1 == 0.0 || norm2 == 0.0) ? 0.0 : dotProduct / (norm1 * norm2);
    }

    public static double singleWordSemanticSimilarity(String word1, String word2) {
        try {
            List<Float> vector1 = FAST_TEXT.getVector(word1);
            List<Float> vector2 = FAST_TEXT.getVector(word2);

            if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
                System.err.println("Word not found in the FastText model: " + word1 + " or " + word2);
                return 0.0;
            }

            return cosineSimilarity(
                    vector1.stream().mapToDouble(Float::doubleValue).toArray(),
                    vector2.stream().mapToDouble(Float::doubleValue).toArray()
            );
        } catch (Exception e) {
            System.err.println("Error calculating similarity: " + e.getMessage());
            return 0.0;
        }
    }
}
