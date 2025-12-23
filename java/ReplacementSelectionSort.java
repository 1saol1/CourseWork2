import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReplacementSelectionSort {
    private static final int REPLACEMENT_BUFFER_SIZE = 30000;
    private static final int PROGRESS_UPDATE_INTERVAL = 500;
    private static final int TIME_UPDATE_INTERVAL = 5000;

    public static void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        gui.updateStatus("Replacement Selection: запуск...");

        gui.updateReplacementProgress(0, "Replacement: создание серий...");
        List<File> series = replacementSelectionSort(inputFile, "replacement_", gui);
        gui.updateReplacementProgress(60, "Replacement: создано " + series.size() + " серий");

        gui.updateReplacementProgress(60, "Replacement: слияние серий...");
        KWayMergeSort.kWayMergeSortWithProgress(series, outputFile, 60, 40, gui);

        cleanupTempFiles(series);
        gui.updateReplacementProgress(100, "Replacement Selection завершен!");
    }

    private static List<File> replacementSelectionSort(String inputFile, String prefix, Coursework gui) throws IOException {
        List<File> outputFiles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile), 8192 * 4)) {
            PriorityQueue<String> currentRun = new PriorityQueue<>();
            PriorityQueue<String> nextRun = new PriorityQueue<>();
            List<String> currentOutput = new ArrayList<>();

            String lastOutput = null;
            long totalWords = 0;
            long processedWords = 0;
            long lastProgressUpdateTime = System.currentTimeMillis();
            int seriesCount = 0;

            totalWords = countWordsInFile(inputFile);

            processedWords = initialBufferLoad(reader, currentRun, REPLACEMENT_BUFFER_SIZE);

            while (!currentRun.isEmpty() || !nextRun.isEmpty()) {
                if (currentRun.isEmpty()) {
                    if (!currentOutput.isEmpty()) {
                        File runFile = createSortedTempFile(currentOutput, prefix + outputFiles.size());
                        outputFiles.add(runFile);
                        currentOutput.clear();
                        seriesCount++;

                        System.gc();
                        try { Thread.sleep(30); } catch (InterruptedException e) { }
                    }

                    PriorityQueue<String> temp = currentRun;
                    currentRun = nextRun;
                    nextRun = temp;
                    lastOutput = null;

                    gui.updateReplacementProgress((int)((processedWords * 60) / totalWords),
                            "Replacement: начата серия " + (outputFiles.size() + 1));
                }

                String minElement = currentRun.poll();

                if (lastOutput == null || minElement.compareTo(lastOutput) >= 0) {
                    currentOutput.add(minElement);
                    lastOutput = minElement;

                    String nextLine = reader.readLine();
                    if (nextLine != null) {
                        String[] words = nextLine.split("\\s+");
                        for (String word : words) {
                            if (!word.trim().isEmpty()) {
                                processedWords++;
                                if (word.compareTo(lastOutput) >= 0) {
                                    currentRun.offer(word);
                                } else {
                                    nextRun.offer(word);
                                }

                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastProgressUpdateTime > 100 || processedWords % PROGRESS_UPDATE_INTERVAL == 0) {
                                    int progress = (int)((processedWords * 60) / totalWords);
                                    gui.updateReplacementProgress(progress,
                                            String.format("Replacement: %d/%d слов (%.1f%%)",
                                                    processedWords, totalWords, (processedWords * 100.0) / totalWords));
                                    lastProgressUpdateTime = currentTime;
                                }
                            }
                        }
                    }
                } else {
                    nextRun.offer(minElement);
                }
            }

            if (!currentOutput.isEmpty()) {
                File runFile = createSortedTempFile(currentOutput, prefix + outputFiles.size());
                outputFiles.add(runFile);
            }
        }

        return outputFiles;
    }

    private static long countWordsInFile(String inputFile) throws IOException {
        long wordCount = 0;
        try (BufferedReader countReader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = countReader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        wordCount++;
                    }
                }
            }
        }
        return wordCount;
    }

    private static long initialBufferLoad(BufferedReader reader, PriorityQueue<String> buffer, int bufferSize) throws IOException {
        long wordsLoaded = 0;
        for (int i = 0; i < bufferSize; i++) {
            String line = reader.readLine();
            if (line == null) break;

            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.trim().isEmpty()) {
                    buffer.offer(word);
                    wordsLoaded++;
                }
            }
        }
        return wordsLoaded;
    }

    private static File createSortedTempFile(List<String> words, String filename) throws IOException {
        Collections.sort(words, String.CASE_INSENSITIVE_ORDER);

        File tempFile = File.createTempFile(filename, ".tmp");
        tempFile.deleteOnExit();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String word : words) {
                writer.write(word);
                writer.newLine();
            }
        }

        return tempFile;
    }

    private static void cleanupTempFiles(List<File> tempFiles) {
        for (File file : tempFiles) {
            try {
                file.delete();
            } catch (Exception e) {
            }
        }
    }
}