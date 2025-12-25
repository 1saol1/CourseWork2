import java.awt.Color;
import java.io.*;
import java.util.*;

public class BucketSort extends BaseExternalSorter {
    private static final Color PROGRESS_COLOR = new Color(150, 0, 150);
    private static final int INITIAL_BUCKETS = 26;
    private static final int MEMORY_THRESHOLD = 1000000;

    @Override
    public void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        // Основной метод сортировки, выбирает стратегию в зависимости от размера файла
        this.gui = gui;
        updateProgress(0, getAlgorithmName() + ": запуск...");

        File input = new File(inputFile);
        if (!input.exists() || input.length() == 0) {
            updateProgress(0, "Bucket Sort: файл не найден или пуст");
            return;
        }

        long fileSize = input.length();
        long totalWords = countWords(inputFile);

        // Выбирает способ сортировки в зависимости от количества слов
        if (totalWords < MEMORY_THRESHOLD) {
            sortInMemory(inputFile, outputFile, totalWords);
        } else {
            sortWithBuckets(inputFile, outputFile, fileSize);
        }

        updateProgress(100, getAlgorithmName() + " завершен!");
        updateTimeLabel();
    }

    private void sortInMemory(String inputFile, String outputFile, long totalWords) throws IOException {
        // Сортирует файл целиком в оперативной памяти
        updateProgress(10, "Загрузка в память...");
        List<String> allWords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            long readWords = 0;

            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        allWords.add(word);
                        readWords++;

                        if (readWords % PROGRESS_UPDATE_INTERVAL == 0) {
                            int progress = 10 + (int)((readWords * 40) / totalWords);
                            updateProgress(progress,
                                    String.format("Загрузка: %d/%d слов", readWords, totalWords));
                        }
                    }
                }
            }
        }

        updateProgress(50, "Сортировка в памяти...");
        // Сортирует все слова без учета регистра
        Collections.sort(allWords, String.CASE_INSENSITIVE_ORDER);

        updateProgress(75, "Запись результата...");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            long writtenWords = 0;
            for (String word : allWords) {
                writer.write(word);
                writer.newLine();
                writtenWords++;

                if (writtenWords % PROGRESS_UPDATE_INTERVAL == 0) {
                    int progress = 75 + (int)((writtenWords * 25) / totalWords);
                    updateProgress(progress,
                            String.format("Запись: %d/%d слов", writtenWords, totalWords));
                }
            }
        }
    }

    private void sortWithBuckets(String inputFile, String outputFile, long fileSize) throws IOException {
        // Сортирует большие файлы с использованием корзин по первой букве
        updateProgress(5, "Статистика по буквам...");
        Map<Character, Long> letterCounts = new HashMap<>();

        // Подсчитывает сколько слов начинается с каждой буквы
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            long processedBytes = 0;

            while ((line = reader.readLine()) != null) {
                processedBytes += line.getBytes().length + 1;
                String[] words = line.split("\\s+");

                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        char firstChar = Character.toLowerCase(word.charAt(0));
                        letterCounts.put(firstChar, letterCounts.getOrDefault(firstChar, 0L) + 1);
                    }
                }

                if (processedBytes % (10 * 1024 * 1024) == 0) {
                    int progress = 5 + (int)((processedBytes * 15) / fileSize);
                    updateProgress(progress, "Статистика...");
                }
            }
        }

        updateProgress(20, "Распределение по временным файлам...");

        // Создает временные файлы для каждой буквы алфавита
        Map<Character, File> tempFiles = new HashMap<>();
        Map<Character, BufferedWriter> writers = new HashMap<>();

        for (char c = 'a'; c <= 'z'; c++) {
            File tempFile = File.createTempFile("bucket_" + c, ".tmp");
            tempFile.deleteOnExit();
            tempFiles.put(c, tempFile);
            writers.put(c, new BufferedWriter(new FileWriter(tempFile)));
        }

        // Распределяет слова по временным файлам в зависимости от первой буквы
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            long processedBytes = 0;
            long wordCount = 0;

            while ((line = reader.readLine()) != null) {
                processedBytes += line.getBytes().length + 1;
                String[] words = line.split("\\s+");

                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        char firstChar = Character.toLowerCase(word.charAt(0));
                        BufferedWriter writer;

                        if (firstChar >= 'a' && firstChar <= 'z') {
                            writer = writers.get(firstChar);
                        } else {
                            writer = writers.get('z'); // Слова не начинающиеся с буквы помещаются в корзину 'z'
                        }

                        writer.write(word);
                        writer.newLine();
                        wordCount++;
                    }
                }

                if (processedBytes % (20 * 1024 * 1024) == 0) {
                    int progress = 20 + (int)((processedBytes * 40) / fileSize);
                    updateProgress(progress,
                            String.format("Распределение: %d слов", wordCount));
                }
            }
        }

        // Закрывает все потоки записи
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }

        updateProgress(60, "Сортировка временных файлов...");

        // Сортирует каждый временный файл и объединяет результаты
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile))) {
            int bucketProcessed = 0;
            for (char c = 'a'; c <= 'z'; c++) {
                File tempFile = tempFiles.get(c);
                if (tempFile.length() > 0) {
                    List<String> bucketWords = new ArrayList<>();

                    // Читает все слова из временного файла
                    try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                bucketWords.add(line);
                            }
                        }
                    }

                    // Сортирует слова в текущей корзине
                    Collections.sort(bucketWords, String.CASE_INSENSITIVE_ORDER);

                    // Записывает отсортированные слова в выходной файл
                    for (String word : bucketWords) {
                        outputWriter.write(word);
                        outputWriter.newLine();
                    }

                    // Удаляет временный файл после обработки
                    tempFile.delete();
                }

                bucketProcessed++;
                int progress = 60 + (bucketProcessed * 40) / 26;
                updateProgress(progress,
                        String.format("Обработка: %d/26 корзин", bucketProcessed));
            }
        }
    }

    private long countWords(String inputFile) throws IOException {
        // Подсчитывает общее количество слов в файле
        long count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public String getAlgorithmName() {
        // Возвращает название алгоритма для отображения
        return "Bucket Sort";
    }

    @Override
    public Color getProgressBarColor() {
        // Возвращает цвет для прогресс-бара этого алгоритма
        return PROGRESS_COLOR;
    }

    @Override
    public String getAlgorithmId() {
        // Возвращает идентификатор алгоритма
        return "bucket";
    }

    @Override
    protected void updateProgress(int value) {
        // Обновляет прогресс в GUI без дополнительного сообщения
        if (gui != null) {
            gui.updateBucketProgress(value);
        }
    }

    @Override
    protected void updateProgress(int value, String status) {
        // Обновляет прогресс в GUI с дополнительным сообщением о статусе
        if (gui != null) {
            if (status != null) {
                gui.updateBucketProgress(value, status);
            } else {
                gui.updateBucketProgress(value);
            }
        }
    }

    @Override
    protected void updateTimeLabel() {
        // Обновляет метку времени в GUI
        if (gui != null) {
            gui.updateBucketTimeLabel();
        }
    }
}