import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ReplacementSelectionSort extends BaseExternalSorter {
    private static final Color PROGRESS_COLOR = new Color(0, 100, 0);
    private static final int REPLACEMENT_BUFFER_SIZE = 100000;

    @Override
    public void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        // Основной метод сортировки с замещающим выбором
        this.gui = gui;
        gui.updateStatus(getAlgorithmName() + ": запуск...");

        // Создает отсортированные серии с помощью алгоритма замещающего выбора
        gui.updateReplacementProgress(0, "Создание серий...");
        List<File> series = replacementSelectionSort(inputFile, gui);
        gui.updateReplacementProgress(60, "Создано " + series.size() + " серий");

        // Сливает созданные серии в один файл
        gui.updateReplacementProgress(60, "Слияние серий...");
        kWayMerge(series, outputFile, gui);

        // Удаляет временные файлы
        cleanupTempFiles(series);
        gui.updateReplacementProgress(100, getAlgorithmName() + " завершен!");
        gui.updateReplacementTimeLabel();
    }

    private List<File> replacementSelectionSort(String inputFile, Coursework gui) throws IOException {
        // Реализует алгоритм замещающего выбора для создания отсортированных серий
        List<File> outputFiles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            PriorityQueue<String> currentRun = new PriorityQueue<>();
            PriorityQueue<String> nextRun = new PriorityQueue<>();
            List<String> currentOutput = new ArrayList<>();

            String lastOutput = null;
            long totalWords = 0;
            long processedWords = 0;

            // Подсчитывает общее количество слов для отслеживания прогресса
            totalWords = countWordsInFile(inputFile);

            // Загружает начальную порцию данных в память
            loadInitialBuffer(reader, currentRun, REPLACEMENT_BUFFER_SIZE);
            processedWords = currentRun.size();

            while (!currentRun.isEmpty() || !nextRun.isEmpty()) {
                if (currentRun.isEmpty()) {
                    // Сохраняет текущую серию в файл и начинает новую
                    if (!currentOutput.isEmpty()) {
                        File runFile = createSortedTempFile(currentOutput, "replacement_series_" + outputFiles.size());
                        outputFiles.add(runFile);
                        currentOutput.clear();
                    }

                    // Переключается на следующую серию
                    PriorityQueue<String> temp = currentRun;
                    currentRun = nextRun;
                    nextRun = temp;
                    lastOutput = null;

                    gui.updateReplacementProgress((int)((processedWords * 60) / totalWords),
                            "Начата серия " + (outputFiles.size() + 1));
                }

                String minElement = currentRun.poll();

                if (lastOutput == null || minElement.compareTo(lastOutput) >= 0) {
                    // Добавляет элемент в текущую серию
                    currentOutput.add(minElement);
                    lastOutput = minElement;

                    // Читает следующий элемент из файла
                    String nextLine = reader.readLine();
                    if (nextLine != null) {
                        String[] words = nextLine.split("\\s+");
                        for (String word : words) {
                            if (!word.trim().isEmpty()) {
                                processedWords++;
                                // Распределяет слово в текущую или следующую серию
                                if (word.compareTo(lastOutput) >= 0) {
                                    currentRun.offer(word);
                                } else {
                                    nextRun.offer(word);
                                }

                                if (processedWords % PROGRESS_UPDATE_INTERVAL == 0) {
                                    gui.updateReplacementProgress((int)((processedWords * 60) / totalWords),
                                            "Обработано " + processedWords + "/" + totalWords + " слов");
                                    gui.updateReplacementTimeLabel();
                                }
                            }
                        }
                    }
                } else {
                    // Отправляет элемент в следующую серию
                    nextRun.offer(minElement);
                }
            }

            // Сохраняет последнюю серию
            if (!currentOutput.isEmpty()) {
                File runFile = createSortedTempFile(currentOutput, "replacement_series_" + outputFiles.size());
                outputFiles.add(runFile);
            }
        }

        return outputFiles;
    }

    private long countWordsInFile(String inputFile) throws IOException {
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

    private void loadInitialBuffer(BufferedReader reader, PriorityQueue<String> buffer, int size) throws IOException {
        // Загружает начальную порцию данных в приоритетную очередь
        while (buffer.size() < size) {
            String line = reader.readLine();
            if (line == null) break;

            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.trim().isEmpty()) {
                    buffer.offer(word);
                    if (buffer.size() >= size) break;
                }
            }
        }
    }

    private void kWayMerge(List<File> chunks, String outputFile, Coursework gui) throws IOException {
        // Выполняет многопутевое слияние серий (аналогично KWayMergeSort)
        if (chunks.isEmpty()) {
            return;
        }

        if (chunks.size() == 1) {
            // Копирует единственную серию напрямую в выходной файл
            Files.copy(chunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            gui.updateReplacementProgress(100);
            return;
        }

        PriorityQueue<FileWord> priorityQueue = new PriorityQueue<>();
        List<BufferedReader> readers = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            // Инициализирует приоритетную очередь первыми словами из каждой серии
            for (File file : chunks) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                readers.add(reader);

                String firstWord = reader.readLine();
                if (firstWord != null) {
                    priorityQueue.offer(new FileWord(firstWord, reader));
                }
            }

            long totalWords = estimateTotalWords(chunks);
            long processedWords = 0;

            gui.updateReplacementProgress(60, "Слияние " + chunks.size() + " серий");

            // Основной цикл многопутевого слияния
            while (!priorityQueue.isEmpty()) {
                FileWord minWord = priorityQueue.poll();
                writer.write(minWord.word);
                writer.newLine();
                processedWords++;

                if (processedWords % PROGRESS_UPDATE_INTERVAL == 0) {
                    int progress = 60 + (int)((processedWords * 40) / totalWords);
                    gui.updateReplacementProgress(progress,
                            "Обработано " + processedWords + "/" + totalWords + " слов");
                    gui.updateReplacementTimeLabel();
                }

                String nextWord = minWord.reader.readLine();
                if (nextWord != null) {
                    priorityQueue.offer(new FileWord(nextWord, minWord.reader));
                }
            }

            gui.updateReplacementProgress(100, "Слияние завершено");
        } finally {
            // Закрывает все открытые потоки чтения
            for (BufferedReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Игнорирует ошибки при закрытии потоков
                }
            }
        }
    }

    @Override
    public String getAlgorithmName() {
        // Возвращает название алгоритма для отображения
        return "Replacement Selection Sort";
    }

    @Override
    public Color getProgressBarColor() {
        // Возвращает цвет для прогресс-бара алгоритма
        return PROGRESS_COLOR;
    }

    @Override
    public String getAlgorithmId() {
        // Возвращает идентификатор алгоритма
        return "replacement";
    }

    @Override
    protected void updateProgress(int value) {
        // Обновляет прогресс в GUI
        gui.updateReplacementProgress(value);
    }

    @Override
    protected void updateProgress(int value, String status) {
        // Обновляет прогресс в GUI с сообщением о статусе
        gui.updateReplacementProgress(value, status);
    }

    @Override
    protected void updateTimeLabel() {
        // Обновляет метку времени в GUI
        gui.updateReplacementTimeLabel();
    }

    private static class FileWord implements Comparable<FileWord> {
        // Вспомогательный класс для хранения слова и соответствующего потока чтения
        String word;
        BufferedReader reader;

        FileWord(String word, BufferedReader reader) {
            this.word = word;
            this.reader = reader;
        }

        @Override
        public int compareTo(FileWord other) {
            // Сравнивает слова без учета регистра для корректной сортировки
            return this.word.compareToIgnoreCase(other.word);
        }
    }
}