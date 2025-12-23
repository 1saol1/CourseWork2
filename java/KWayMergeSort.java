import java.io.*;
import java.nio.file.*;
import java.util.*;

public class KWayMergeSort {
    private static final int MAX_MEMORY_SIZE = 30 * 1024 * 1024;
    private static final int MAX_WORDS_PER_CHUNK = 50000;
    private static final int PROGRESS_UPDATE_INTERVAL = 5000;

    public static void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        gui.updateStatus("K-Way: запуск...");

        gui.updateKWayProgress(0, "K-Way: разделение файла...");
        List<File> chunks = splitAndSortFiles(inputFile, "chunk2_", 0, 50, gui);
        gui.updateKWayProgress(50, "K-Way: файл разделен на " + chunks.size() + " чанков");

        gui.updateKWayProgress(50, "K-Way: начало многопутевого слияния...");
        kWayMergeSortWithProgress(chunks, outputFile, 50, 50, gui);

        cleanupTempFiles(chunks);
        gui.updateKWayProgress(100, "K-Way завершен!");
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
        long lastUpdateTime = System.currentTimeMillis();
        int chunkCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192 * 4)) {
            List<String> words = new ArrayList<>();
            String line;
            int count = 1;
            long currentChunkSize = 0;
            int wordCount = 0;

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
                            gui.updateKWayProgress(progress,
                                    String.format("K-Way: обработано %d слов (%.1f%%)",
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

                            gui.updateKWayProgress(progressStart + (int)((processedBytes * progressRange) / fileSize),
                                    "K-Way: создан чанк " + (count-1));

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
                gui.updateKWayProgress(progressStart + progressRange, "K-Way: создан финальный чанк");
            }
        }

        return tempFiles;
    }

    static void kWayMergeSortWithProgress(List<File> chunks, String outputFile,
                                          int progressStart, int progressRange, Coursework gui) throws IOException {
        if (chunks.isEmpty()) {
            return;
        }

        if (chunks.size() == 1) {
            Files.copy(chunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            if (progressStart + progressRange <= 100) {
                gui.updateKWayProgress(progressStart + progressRange);
            }
            return;
        }

        PriorityQueue<FileWord> pq = new PriorityQueue<>(Math.min(chunks.size(), 1000));
        List<BufferedReader> readers = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            for (File file : chunks) {
                BufferedReader reader = new BufferedReader(new FileReader(file), 8192 * 4);
                readers.add(reader);

                String firstWord = reader.readLine();
                if (firstWord != null) {
                    pq.offer(new FileWord(firstWord, reader));
                }
            }

            long totalWords = estimateTotalWords(chunks);
            long processedWords = 0;
            long lastUpdateTime = System.currentTimeMillis();
            int gcCounter = 0;

            gui.updateKWayProgress(progressStart, "K-Way: начало слияния " + chunks.size() + " чанков");

            while (!pq.isEmpty()) {
                FileWord minWord = pq.poll();
                writer.write(minWord.word);
                writer.newLine();
                processedWords++;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > 100 || processedWords % 5000 == 0) {
                    int progress = progressStart + (int)((processedWords * progressRange) / totalWords);
                    gui.updateKWayProgress(Math.min(progressStart + progressRange, progress),
                            String.format("K-Way: слияние %d/%d слов (%.1f%%)",
                                    processedWords, totalWords, (processedWords * 100.0) / totalWords));
                    lastUpdateTime = currentTime;

                    gcCounter++;
                    if (gcCounter % 10000 == 0) {
                        System.gc();
                    }
                }

                String nextWord = minWord.reader.readLine();
                if (nextWord != null) {
                    pq.offer(new FileWord(nextWord, minWord.reader));
                }
            }

            gui.updateKWayProgress(progressStart + progressRange, "K-Way: слияние завершено");
        } finally {
            for (BufferedReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
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

    private static long estimateTotalWords(List<File> files) throws IOException {
        long total = 0;
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                while (reader.readLine() != null) {
                    total++;
                }
            }
        }
        return total;
    }

    private static void cleanupTempFiles(List<File> tempFiles) {
        for (File file : tempFiles) {
            file.delete();
        }
    }

    private static int getMemorySize(String word) {
        return word.getBytes().length;
    }

    static class FileWord implements Comparable<FileWord> {
        String word;
        BufferedReader reader;

        FileWord(String word, BufferedReader reader) {
            this.word = word;
            this.reader = reader;
        }

        @Override
        public int compareTo(FileWord other) {
            return this.word.compareToIgnoreCase(other.word);
        }
    }
}