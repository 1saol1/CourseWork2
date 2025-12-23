import java.io.*;
import java.util.*;

public class BucketSort {
    private static final int BUCKET_COUNT = 32;
    private static final int MAX_WORDS_PER_BUCKET = 30000;
    private static final int PROGRESS_UPDATE_INTERVAL = 5000;

    public static void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        gui.updateStatus("Bucket Sort: запуск...");
        gui.updateBucketProgress(0, "Bucket Sort: анализ данных...");

        int maxWordLength = findMaxWordLength(inputFile);
        if (maxWordLength == -1) {
            gui.updateStatus("Bucket Sort: файл не найден или пуст");
            return;
        }

        gui.updateBucketProgress(20, "Bucket Sort: распределение по корзинам...");
        List<File> bucketFiles = distributeToBuckets(inputFile, maxWordLength, gui);

        gui.updateBucketProgress(50, "Bucket Sort: сортировка корзин...");
        sortAllBuckets(bucketFiles, gui);

        gui.updateBucketProgress(80, "Bucket Sort: объединение корзин...");
        mergeBucketsToFile(bucketFiles, outputFile, gui);

        gui.updateBucketProgress(95, "Bucket Sort: очистка временных файлов...");
        cleanupTempFiles(bucketFiles);

        gui.updateBucketProgress(100, "Bucket Sort завершен!");
    }

    private static int findMaxWordLength(String inputFile) throws IOException {
        File file = new File(inputFile);
        if (!file.exists()) {
            return -1;
        }

        int max = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        if (word.length() > max) {
                            max = word.length();
                        }
                    }
                }
            }
        }
        return max;
    }

    private static List<File> distributeToBuckets(String inputFile, int maxWordLength, Coursework gui) throws IOException {
        List<File> bucketFiles = new ArrayList<>();
        List<BufferedWriter> writers = new ArrayList<>();

        for (int i = 0; i < BUCKET_COUNT; i++) {
            File bucketFile = File.createTempFile("bucket_" + i, ".tmp");
            bucketFile.deleteOnExit();
            bucketFiles.add(bucketFile);
            writers.add(new BufferedWriter(new FileWriter(bucketFile)));
        }

        File file = new File(inputFile);
        long fileSize = file.length();
        long processedBytes = 0;
        long wordCount = 0;
        long lastUpdateTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192 * 4)) {
            String line;
            while ((line = reader.readLine()) != null) {
                processedBytes += line.getBytes().length + 1;
                wordCount++;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > 100 || wordCount % PROGRESS_UPDATE_INTERVAL == 0) {
                    int progress = 20 + (int)((processedBytes * 30) / fileSize);
                    gui.updateBucketProgress(progress,
                            String.format("Bucket Sort: распределение %d слов (%.1f%%)",
                                    wordCount, (processedBytes * 100.0) / fileSize));
                    lastUpdateTime = currentTime;
                }

                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        int bucketIndex = (word.length() * BUCKET_COUNT) / (maxWordLength + 1);
                        if (bucketIndex >= BUCKET_COUNT) bucketIndex = BUCKET_COUNT - 1;

                        writers.get(bucketIndex).write(word + "\n");
                    }
                }
            }
        }

        for (BufferedWriter writer : writers) {
            writer.close();
        }

        return bucketFiles;
    }

    private static void sortAllBuckets(List<File> bucketFiles, Coursework gui) throws IOException {
        int totalBuckets = bucketFiles.size();
        for (int i = 0; i < totalBuckets; i++) {
            File bucketFile = bucketFiles.get(i);

            sortBucketFileInPlace(bucketFile);

            int progress = 50 + (i * 30) / totalBuckets;
            gui.updateBucketProgress(progress,
                    String.format("Bucket Sort: отсортирована корзина %d/%d", i + 1, totalBuckets));

            if (i % 2 == 0) {
                System.gc();
                try { Thread.sleep(20); } catch (InterruptedException e) { }
            }
        }
    }

    private static void sortBucketFileInPlace(File bucketFile) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(bucketFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                    if (lines.size() >= MAX_WORDS_PER_BUCKET) {
                        Collections.sort(lines, String.CASE_INSENSITIVE_ORDER);
                        writeBucketToFile(lines, bucketFile);
                        lines.clear();
                        System.gc();
                    }
                }
            }
        }

        if (!lines.isEmpty()) {
            Collections.sort(lines, String.CASE_INSENSITIVE_ORDER);
            writeBucketToFile(lines, bucketFile);
        }
    }

    private static void writeBucketToFile(List<String> data, File bucketFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(bucketFile))) {
            for (String word : data) {
                writer.write(word + "\n");
            }
        }
    }

    private static void mergeBucketsToFile(List<File> bucketFiles, String outputFile, Coursework gui) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            long totalElements = estimateTotalLines(bucketFiles);
            long processedElements = 0;
            long lastUpdateTime = System.currentTimeMillis();

            for (File bucketFile : bucketFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(bucketFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        writer.write(line + "\n");
                        processedElements++;

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime > 100 || processedElements % 5000 == 0) {
                            int progress = 80 + (int)((processedElements * 15) / totalElements);
                            gui.updateBucketProgress(progress,
                                    String.format("Bucket Sort: объединение %d/%d слов (%.1f%%)",
                                            processedElements, totalElements, (processedElements * 100.0) / totalElements));
                            lastUpdateTime = currentTime;
                        }
                    }
                }
            }
        }
    }

    private static long estimateTotalLines(List<File> files) throws IOException {
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
}