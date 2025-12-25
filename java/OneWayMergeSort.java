import javax.swing.*;
import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OneWayMergeSort extends BaseExternalSorter {
    private static final Color PROGRESS_COLOR = new Color(0, 0, 150);

    @Override
    public void sort(String inputFile, String outputFile, Coursework gui) throws IOException {
        // Основной метод сортировки однонаправленным слиянием
        this.gui = gui;
        gui.updateStatus(getAlgorithmName() + ": запуск...");

        // Разделяет файл на отсортированные части
        gui.updateOneWayProgress(0, "Разделение файла...");
        List<File> chunks = splitFileIntoSortedChunks(inputFile, "oneway_chunk_", 0, 50);
        gui.updateOneWayProgress(50, "Файл разделен на " + chunks.size() + " чанков");

        // Выполняет слияние частей попарно
        gui.updateOneWayProgress(50, "Начало слияния...");
        mergeChunks(chunks, outputFile, gui);

        // Удаляет временные файлы
        cleanupTempFiles(chunks);
        gui.updateOneWayProgress(100, getAlgorithmName() + " завершен!");
        gui.updateOneWayTimeLabel();
    }

    private void mergeChunks(List<File> chunks, String outputFile, Coursework gui) throws IOException {
        // Рекурсивно сливает отсортированные чанки попарно
        if (chunks.isEmpty()) {
            return;
        }

        if (chunks.size() == 1) {
            // Копирует единственный чанк напрямую в выходной файл
            Files.copy(chunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            gui.updateOneWayProgress(100);
            return;
        }

        List<File> currentFiles = new ArrayList<>(chunks);
        int totalMerges = currentFiles.size() - 1;
        int completedMerges = 0;
        int stage = 1;

        // Последовательно сливает файлы попарно пока не останется один файл
        while (currentFiles.size() > 1) {
            gui.updateOneWayProgress(50 + (completedMerges * 50) / totalMerges,
                    "Этап " + stage + " (файлов: " + currentFiles.size() + ")");

            List<File> mergedFiles = new ArrayList<>();
            for (int i = 0; i < currentFiles.size(); i += 2) {
                if (i + 1 < currentFiles.size()) {
                    // Сливает два соседних файла
                    File mergedFile = mergeTwoFiles(currentFiles.get(i), currentFiles.get(i + 1),
                            "oneway_merge_" + stage + "_" + i);
                    mergedFiles.add(mergedFile);
                    completedMerges++;
                } else {
                    // Если нечетное количество файлов, оставляет последний без изменений
                    mergedFiles.add(currentFiles.get(i));
                }

                gui.updateOneWayProgress(50 + (completedMerges * 50) / totalMerges,
                        "Слияние " + completedMerges + "/" + totalMerges);
                gui.updateOneWayTimeLabel();
            }
            currentFiles = mergedFiles;
            stage++;
        }

        // Перемещает финальный слитый файл в указанное место
        if (!currentFiles.isEmpty()) {
            Files.move(currentFiles.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File mergeTwoFiles(File file1, File file2, String tempName) throws IOException {
        // Сливает два отсортированных файла в один отсортированный файл
        File tempFile = File.createTempFile(tempName, ".tmp");
        tempFile.deleteOnExit();

        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1));
             BufferedReader reader2 = new BufferedReader(new FileReader(file2));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String word1 = reader1.readLine();
            String word2 = reader2.readLine();

            // Основной цикл слияния: выбирает минимальное слово из двух файлов
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

            // Дозаписывает оставшиеся слова из первого файла
            while (word1 != null) {
                writer.write(word1);
                writer.newLine();
                word1 = reader1.readLine();
            }

            // Дозаписывает оставшиеся слова из второго файла
            while (word2 != null) {
                writer.write(word2);
                writer.newLine();
                word2 = reader2.readLine();
            }
        }

        // Удаляет исходные файлы после слияния
        file1.delete();
        file2.delete();

        return tempFile;
    }

    @Override
    public String getAlgorithmName() {
        // Возвращает название алгоритма для отображения
        return "One-Way Merge Sort";
    }

    @Override
    public Color getProgressBarColor() {
        // Возвращает цвет для прогресс-бара алгоритма
        return PROGRESS_COLOR;
    }

    @Override
    public String getAlgorithmId() {
        // Возвращает идентификатор алгоритма
        return "one_way";
    }

    @Override
    protected void updateProgress(int value) {
        // Обновляет прогресс в GUI
        gui.updateOneWayProgress(value);
    }

    @Override
    protected void updateProgress(int value, String status) {
        // Обновляет прогресс в GUI с сообщением о статусе
        gui.updateOneWayProgress(value, status);
    }

    @Override
    protected void updateTimeLabel() {
        // Обновляет метку времени в GUI
        gui.updateOneWayTimeLabel();
    }
}