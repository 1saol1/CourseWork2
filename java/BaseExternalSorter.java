import java.awt.Color;
import java.io.*;
import java.util.*;

public abstract class BaseExternalSorter implements ExternalSortAlgorithm {
    // Максимальный размер данных для обработки в памяти
    protected static final int MAX_MEMORY_SIZE = 250 * 1024 * 1024;
    // Интервал обновления прогресса (количество обработанных элементов)
    protected static final int PROGRESS_UPDATE_INTERVAL = 10000;

    // Ссылка на GUI для обновления прогресса
    protected Coursework gui;

    public BaseExternalSorter() {
        // Конструктор по умолчанию
    }

    public void setGUI(Coursework gui) {
        // Устанавливает ссылку на GUI для обновления прогресса
        this.gui = gui;
    }

    protected File createSortedTempFile(List<String> words, String filename) throws IOException {
        // Сортирует список слов и сохраняет их во временный файл
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

    protected void cleanupTempFiles(List<File> tempFiles) {
        // Удаляет все временные файлы из списка
        for (File file : tempFiles) {
            try {
                file.delete();
            } catch (Exception e) {
                // Игнорирует ошибки при удалении файлов
                System.err.println("Не удалось удалить временный файл: " + file.getName());
            }
        }
    }

    protected int getMemorySize(String word) {
        // Вычисляет размер слова в байтах
        return word.getBytes().length;
    }

    protected List<String> readChunkFromFile(BufferedReader reader, long maxBytes) throws IOException {
        // Читает порцию данных из файла, не превышая заданный лимит памяти
        List<String> chunk = new ArrayList<>();
        long currentSize = 0;
        String line;

        while ((line = reader.readLine()) != null && currentSize < maxBytes) {
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.trim().isEmpty()) {
                    int wordSize = getMemorySize(word);
                    if (currentSize + wordSize <= maxBytes) {
                        chunk.add(word);
                        currentSize += wordSize;
                    } else {
                        // Прекращает чтение при достижении лимита памяти
                        return chunk;
                    }
                }
            }
        }

        return chunk;
    }

    protected List<File> splitFileIntoSortedChunks(String inputFile, String chunkPrefix,
                                                   int progressStart, int progressRange) throws IOException {
        // Разделяет файл на отсортированные части (чанки) для дальнейшей обработки
        File file = new File(inputFile);
        List<File> tempFiles = new ArrayList<>();

        if (!file.exists()) {
            if (gui != null) {
                gui.updateStatus("ОШИБКА: Файл не найден - " + inputFile);
            }
            return tempFiles;
        }

        long fileSize = file.length();
        long processedBytes = 0;
        int chunkCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> chunk = new ArrayList<>();
            long currentChunkSize = 0;

            while (true) {
                List<String> newWords = readChunkFromFile(reader, MAX_MEMORY_SIZE - currentChunkSize);
                if (newWords.isEmpty()) {
                    break;
                }

                chunk.addAll(newWords);
                currentChunkSize += calculateChunkSize(newWords);
                processedBytes += calculateChunkSize(newWords);

                // Обновляет прогресс чтения файла
                int progress = progressStart + (int)((processedBytes * progressRange) / fileSize);
                updateProgress(progress);

                // Создает новый чанк при заполнении памяти или малом количестве слов
                if (currentChunkSize >= MAX_MEMORY_SIZE * 0.9 || newWords.size() < 1000) {
                    if (!chunk.isEmpty()) {
                        File tempFile = createSortedTempFile(chunk, chunkPrefix + (++chunkCount));
                        tempFiles.add(tempFile);

                        if (gui != null) {
                            gui.updateStatus(getAlgorithmName() + ": создан чанк " + chunkCount);
                        }

                        chunk.clear();
                        currentChunkSize = 0;
                    }
                }

                updateTimeLabel();
            }

            // Обрабатывает оставшиеся слова как последний чанк
            if (!chunk.isEmpty()) {
                File tempFile = createSortedTempFile(chunk, chunkPrefix + (++chunkCount));
                tempFiles.add(tempFile);
                if (gui != null) {
                    gui.updateStatus(getAlgorithmName() + ": создан финальный чанк");
                }
            }
        }

        updateProgress(progressStart + progressRange);
        return tempFiles;
    }

    private long calculateChunkSize(List<String> words) {
        // Вычисляет общий размер списка слов в байтах
        long size = 0;
        for (String word : words) {
            size += getMemorySize(word);
        }
        return size;
    }

    protected long estimateTotalWords(List<File> files) throws IOException {
        // Оценивает общее количество слов в списке файлов
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

    protected abstract void updateProgress(int value);
    protected abstract void updateTimeLabel();

    protected void updateProgress(int value, String status) {
        // Обновляет прогресс с дополнительным сообщением о статусе
        updateProgress(value);
    }

    public abstract Color getProgressBarColor();
}