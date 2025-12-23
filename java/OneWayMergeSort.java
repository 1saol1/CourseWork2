import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OneWayMergeSort {
    private static final int MAX_MEMORY_SIZE = 30 * 1024 * 1024;
    private static final int MAX_WORDS_PER_CHUNK = 50000;
    private static final int PROGRESS_UPDATE_INTERVAL = 5000;

    public static void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        gui.updateStatus("One-Way: запуск...");

        gui.updateOneWayProgress(0, "One-Way: разделение файла...");
        List<File> chunks = splitAndSortFiles(inputFile, "chunk1_", 0, 50, gui);
        gui.updateOneWayProgress(50, "One-Way: файл разделен на " + chunks.size() + " чанков");

        gui.updateOneWayProgress(50, "One-Way: начало слияния...");
        oneWayMergeSortWithProgress(chunks, outputFile, 50, 50, gui);

        cleanupTempFiles(chunks);
        gui.updateOneWayProgress(100, "One-Way завершен!");
    }

    private static List<File> splitAndSortFiles(String inputFile, String chunkPrefix,
                                                int progressStart, int progressRange, Coursework gui) throws IOException {
        File file = new File(inputFile);
        List<File> tempFiles = new ArrayList<>();

        if (!file.exists()) {
            gui.updateStatus("ОШИБКА: Файл не найден - " + inputFile);
            return tempFiles;
        }

        long fileSize = file.length();
        long processedBytes = 0;
        long wordCount = 0;
        long lastUpdateTime = System.currentTimeMillis();
        int chunkCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192 * 4)) {
            List<String> words = new ArrayList<>();
            String line;
            int count = 1;
            long currentChunkSize = 0;

            while ((line = reader.readLine()) != null) {
                String[] lineWords = line.split("\\s+");
                for (String word : lineWords) {
                    if (!word.trim().isEmpty()) {
                        int wordSize = getMemorySize(word);
                        processedBytes += wordSize;
                        wordCount++;

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime > 100 || wordCount % PROGRESS_UPDATE_INTERVAL == 0) {
                            int progress = progressStart + (int)((processedBytes * progressRange) / fileSize);
                            gui.updateOneWayProgress(progress,
                                    String.format("One-Way: обработано %d слов (%.1f%%)",
                                            wordCount, (processedBytes * 100.0) / fileSize));
                            lastUpdateTime = currentTime;
                        }

                        if (currentChunkSize + wordSize <= MAX_MEMORY_SIZE && words.size() < MAX_WORDS_PER_CHUNK) {
                            words.add(word);
                            currentChunkSize += wordSize;
                        } else {
                            File tempFile = createSortedTempFile(words, chunkPrefix + count++);
                            tempFiles.add(tempFile);
                            chunkCount++;

                            words.clear();
                            words.add(word);
                            currentChunkSize = wordSize;

                            gui.updateOneWayProgress(progressStart + (int)((processedBytes * progressRange) / fileSize),
                                    "One-Way: создан чанк " + (count-1));

                            if (chunkCount % 3 == 0) {
                                System.gc();
                                try { Thread.sleep(30); } catch (InterruptedException e) { }
                            }
                        }
                    }
                }
            }

            if (!words.isEmpty()) {
                File tempFile = createSortedTempFile(words, chunkPrefix + count);
                tempFiles.add(tempFile);
                gui.updateOneWayProgress(progressStart + progressRange, "One-Way: создан финальный чанк");
            }
        }

        return tempFiles;
    }

    private static void oneWayMergeSortWithProgress(List<File> chunks, String outputFile,
                                                    int progressStart, int progressRange, Coursework gui) throws IOException {
        if (chunks.isEmpty()) {
            return;
        }

        List<File> currentFiles = new ArrayList<>(chunks);
        int totalMerges = currentFiles.size() - 1;
        int completedMerges = 0;
        int stage = 1;
        long lastUpdateTime = System.currentTimeMillis();
        int mergeCount = 0;

        while (currentFiles.size() > 1) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 200) {
                gui.updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                        String.format("One-Way: этап %d (%d файлов -> %d)",
                                stage, currentFiles.size(), (currentFiles.size() + 1) / 2));
                lastUpdateTime = currentTime;
            }

            List<File> mergedFiles = new ArrayList<>();
            for (int i = 0; i < currentFiles.size(); i += 2) {
                if (i + 1 < currentFiles.size()) {
                    File mergedFile = mergeTwoFiles(currentFiles.get(i), currentFiles.get(i + 1),
                            "one_way_temp_" + stage + "_" + i);
                    mergedFiles.add(mergedFile);
                    completedMerges++;
                    mergeCount++;
                } else {
                    mergedFiles.add(currentFiles.get(i));
                }

                gui.updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                        String.format("One-Way: слияние %d/%d", completedMerges, totalMerges));

                if (mergeCount % 2 == 0) {
                    System.gc();
                }
            }
            currentFiles = mergedFiles;
            stage++;
        }

        if (!currentFiles.isEmpty()) {
            Files.move(currentFiles.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
        }
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

    private static File mergeTwoFiles(File file1, File file2, String tempName) throws IOException {
        File tempFile = File.createTempFile(tempName, ".tmp");
        tempFile.deleteOnExit();

        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1), 8192 * 4);
             BufferedReader reader2 = new BufferedReader(new FileReader(file2), 8192 * 4);
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String word1 = reader1.readLine();
            String word2 = reader2.readLine();

            while (word1 != null && word2 != null) {
                if (word1.compareToIgnoreCase(word2) <= 0) {
                    writer.write(word1);
                    writer.newLine();
                    word1 = reader1.readLine();
                } else {
                    writer.write(word2);
                    writer.newLine();
                    word2 = reader2.readLine();
                }
            }

            while (word1 != null) {
                writer.write(word1);
                writer.newLine();
                word1 = reader1.readLine();
            }

            while (word2 != null) {
                writer.write(word2);
                writer.newLine();
                word2 = reader2.readLine();
            }
        }

        file1.delete();
        file2.delete();

        return tempFile;
    }

    private static void cleanupTempFiles(List<File> tempFiles) {
        for (File file : tempFiles) {
            file.delete();
        }
    }

    private static int getMemorySize(String word) {
        return word.getBytes().length;
    }
}