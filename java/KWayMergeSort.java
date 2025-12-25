import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class KWayMergeSort extends BaseExternalSorter {
    private static final Color PROGRESS_COLOR = new Color(150, 0, 0);

    @Override
    public void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        // Основной метод сортировки K-Way слиянием
        this.gui = gui;
        gui.updateStatus(getAlgorithmName() + ": запуск...");

        // Разделяет файл на отсортированные части
        gui.updateKWayProgress(0, "Разделение файла...");
        List<File> chunks = splitFileIntoSortedChunks(inputFile, "kway_chunk_", 0, 50);
        gui.updateKWayProgress(50, "Файл разделен на " + chunks.size() + " чанков");

        // Выполняет многопутевое слияние частей
        gui.updateKWayProgress(50, "Начало многопутевого слияния...");
        kWayMerge(chunks, outputFile, gui);

        // Удаляет временные файлы
        cleanupTempFiles(chunks);
        gui.updateKWayProgress(100, getAlgorithmName() + " завершен!");
        gui.updateKWayTimeLabel();
    }

    private void kWayMerge(List<File> chunks, String outputFile, Coursework gui) throws IOException {
        // Выполняет многопутевое слияние отсортированных чанков
        if (chunks.isEmpty()) {
            return;
        }

        if (chunks.size() == 1) {
            // Копирует единственный чанк напрямую в выходной файл
            Files.copy(chunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            gui.updateKWayProgress(100);
            return;
        }

        PriorityQueue<FileWord> priorityQueue = new PriorityQueue<>();
        List<BufferedReader> readers = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            // Инициализирует приоритетную очередь первыми словами из каждого чанка
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

            gui.updateKWayProgress(50, "Слияние " + chunks.size() + " чанков");

            // Основной цикл слияния: извлекает минимальный элемент и добавляет следующий
            while (!priorityQueue.isEmpty()) {
                FileWord minWord = priorityQueue.poll();
                writer.write(minWord.word);
                writer.newLine();
                processedWords++;

                if (processedWords % PROGRESS_UPDATE_INTERVAL == 0) {
                    int progress = 50 + (int)((processedWords * 50) / totalWords);
                    gui.updateKWayProgress(progress,
                            "Обработано " + processedWords + "/" + totalWords + " слов");
                    gui.updateKWayTimeLabel();
                }

                String nextWord = minWord.reader.readLine();
                if (nextWord != null) {
                    priorityQueue.offer(new FileWord(nextWord, minWord.reader));
                }
            }

            gui.updateKWayProgress(100, "Слияние завершено");
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
        return "K-Way Merge Sort";
    }

    @Override
    public Color getProgressBarColor() {
        // Возвращает цвет для прогресс-бара алгоритма
        return PROGRESS_COLOR;
    }

    @Override
    public String getAlgorithmId() {
        // Возвращает идентификатор алгоритма
        return "k_way";
    }

    @Override
    protected void updateProgress(int value) {
        // Обновляет прогресс в GUI
        gui.updateKWayProgress(value);
    }

    @Override
    protected void updateProgress(int value, String status) {
        // Обновляет прогресс в GUI с сообщением о статусе
        gui.updateKWayProgress(value, status);
    }

    @Override
    protected void updateTimeLabel() {
        // Обновляет метку времени в GUI
        gui.updateKWayTimeLabel();
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