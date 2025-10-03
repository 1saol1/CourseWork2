import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        SortOptions options = new SortOptions();
        options.addInputFilePath("file1_8gb.txt");
        options.addInputFilePath("file2_8gb.txt");
        options.setOutputFilePath("final_sorted_16gb.txt");
        options.setMaxChunkSizeInBytes(100 * 1024 * 1024); // 100 MB

        // Создаём тестовые файлы, если их нет
        createSampleFilesIfNeeded(options);

        // Создаём сортировщик
        ExternalMergeSorter<String> sorter = new ExternalMergeSorter<>(
                options,
                new DefaultInMemorySorter<String>()
        );

        try {
            long startTime = System.currentTimeMillis();
            sorter.sortMultipleFiles();
            long endTime = System.currentTimeMillis();

            System.out.println("Общее время выполнения: " + (endTime - startTime) + " мс");

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createSampleFilesIfNeeded(SortOptions options) {
        for (String filePath : options.getInputFilePaths()) {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Создание тестового файла: " + filePath);
                createSampleInputFile(filePath, 10000); // 10,000 строк для теста
            }
        }
    }

    private static void createSampleInputFile(String filePath, int lineCount) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            Random random = new Random();

            for (int i = 0; i < lineCount; i++) {
                int number = random.nextInt(1000000);
                writer.println(number);
            }

            System.out.println("Создан файл " + filePath + " с " + lineCount + " случайными числами");

        } catch (IOException e) {
            System.err.println("Ошибка при создании тестового файла: " + e.getMessage());
        }
    }
}