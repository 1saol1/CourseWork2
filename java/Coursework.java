import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Coursework extends JFrame {
    private JProgressBar oneWayProgressBar;
    private JProgressBar kWayProgressBar;
    private JProgressBar replacementProgressBar;
    private JProgressBar bucketProgressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel oneWayTimeLabel;
    private JLabel kWayTimeLabel;
    private JLabel replacementTimeLabel;
    private JLabel bucketTimeLabel;
    private JButton startButton;

    private long startTime;
    private long oneWayStartTime;
    private long kWayStartTime;
    private long replacementStartTime;
    private long bucketStartTime;
    private volatile boolean isRunning = false;

    private static final int MAX_MEMORY_SIZE = 250 * 1024 * 1024;
    private static final int REPLACEMENT_BUFFER_SIZE = 100000;

    public Coursework() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Сравнение сортировок - One-Way vs K-Way vs Replacement Selection vs Bucket Sort");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Управление сортировкой"));

        startButton = new JButton("Начать сортировки");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(180, 35));
        controlPanel.add(startButton);

        JPanel progressPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        progressPanel.setBorder(BorderFactory.createTitledBorder("Прогресс выполнения"));

        JPanel oneWayPanel = new JPanel(new BorderLayout(5, 5));
        oneWayPanel.add(new JLabel("One-Way Merge Sort:"), BorderLayout.NORTH);
        oneWayProgressBar = new JProgressBar(0, 100);
        oneWayProgressBar.setStringPainted(true);
        oneWayProgressBar.setForeground(new Color(0, 0, 150));
        oneWayProgressBar.setPreferredSize(new Dimension(400, 20));
        oneWayPanel.add(oneWayProgressBar, BorderLayout.CENTER);
        oneWayTimeLabel = new JLabel("Время: 0 сек");
        oneWayTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        oneWayPanel.add(oneWayTimeLabel, BorderLayout.SOUTH);

        JPanel kWayPanel = new JPanel(new BorderLayout(5, 5));
        kWayPanel.add(new JLabel("K-Way Merge Sort:"), BorderLayout.NORTH);
        kWayProgressBar = new JProgressBar(0, 100);
        kWayProgressBar.setStringPainted(true);
        kWayProgressBar.setForeground(new Color(150, 0, 0));
        kWayProgressBar.setPreferredSize(new Dimension(400, 20));
        kWayPanel.add(kWayProgressBar, BorderLayout.CENTER);
        kWayTimeLabel = new JLabel("Время: 0 сек");
        kWayTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        kWayPanel.add(kWayTimeLabel, BorderLayout.SOUTH);

        JPanel replacementPanel = new JPanel(new BorderLayout(5, 5));
        replacementPanel.add(new JLabel("Replacement Selection Sort:"), BorderLayout.NORTH);
        replacementProgressBar = new JProgressBar(0, 100);
        replacementProgressBar.setStringPainted(true);
        replacementProgressBar.setForeground(new Color(0, 100, 0));
        replacementProgressBar.setPreferredSize(new Dimension(400, 20));
        replacementPanel.add(replacementProgressBar, BorderLayout.CENTER);
        replacementTimeLabel = new JLabel("Время: 0 сек");
        replacementTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        replacementPanel.add(replacementTimeLabel, BorderLayout.SOUTH);

        JPanel bucketPanel = new JPanel(new BorderLayout(5, 5));
        bucketPanel.add(new JLabel("Bucket Sort:"), BorderLayout.NORTH);
        bucketProgressBar = new JProgressBar(0, 100);
        bucketProgressBar.setStringPainted(true);
        bucketProgressBar.setForeground(new Color(150, 0, 150));
        bucketProgressBar.setPreferredSize(new Dimension(400, 20));
        bucketPanel.add(bucketProgressBar, BorderLayout.CENTER);
        bucketTimeLabel = new JLabel("Время: 0 сек");
        bucketTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        bucketPanel.add(bucketTimeLabel, BorderLayout.SOUTH);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusLabel = new JLabel("Готов к работе");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        timeLabel = new JLabel("Общее время: 0 сек");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        infoPanel.add(statusLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(timeLabel);

        progressPanel.add(oneWayPanel);
        progressPanel.add(kWayPanel);
        progressPanel.add(replacementPanel);
        progressPanel.add(bucketPanel);
        progressPanel.add(infoPanel);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(progressPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> startSorting());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startSorting() {
        if (isRunning) return;

        startButton.setEnabled(false);
        isRunning = true;

        oneWayProgressBar.setValue(0);
        kWayProgressBar.setValue(0);
        replacementProgressBar.setValue(0);
        bucketProgressBar.setValue(0);
        oneWayTimeLabel.setText("Время: 0 сек");
        kWayTimeLabel.setText("Время: 0 сек");
        replacementTimeLabel.setText("Время: 0 сек");
        bucketTimeLabel.setText("Время: 0 сек");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                startTime = System.currentTimeMillis();
                oneWayStartTime = 0;
                kWayStartTime = 0;
                replacementStartTime = 0;
                bucketStartTime = 0;

                updateStatus("Запуск всех сортировок...");
                updateTimeLabel();

                Thread oneWayThread = new Thread(() -> {
                    try {
                        oneWayStartTime = System.currentTimeMillis();
                        runOneWayMergeSort();
                    } catch (IOException e) {
                        updateStatus("Ошибка в One-Way сортировке: " + e.getMessage());
                    }
                });

                Thread kWayThread = new Thread(() -> {
                    try {
                        kWayStartTime = System.currentTimeMillis();
                        runKWayMergeSort();
                    } catch (IOException e) {
                        updateStatus("Ошибка в K-Way сортировке: " + e.getMessage());
                    }
                });

                Thread replacementThread = new Thread(() -> {
                    try {
                        replacementStartTime = System.currentTimeMillis();
                        runReplacementSelectionSort();
                    } catch (IOException e) {
                        updateStatus("Ошибка в Replacement Selection: " + e.getMessage());
                    }
                });

                Thread bucketThread = new Thread(() -> {
                    try {
                        bucketStartTime = System.currentTimeMillis();
                        runBucketSort();
                    } catch (IOException e) {
                        updateStatus("Ошибка в Bucket сортировке: " + e.getMessage());
                    }
                });

                oneWayThread.start();
                kWayThread.start();
                replacementThread.start();
                bucketThread.start();

                oneWayThread.join();
                kWayThread.join();
                replacementThread.join();
                bucketThread.join();

                return null;
            }

            @Override
            protected void done() {
                isRunning = false;
                startButton.setEnabled(true);
                updateStatus("Все сортировки завершены!");
                updateTimeLabel();
            }
        };
        worker.execute();
    }

    private void runBucketSort() throws IOException {
        updateStatus("Bucket Sort: запуск...");
        String inputFile = "large_file.txt";
        String outputFile = "sorted_bucket.txt";

        updateBucketProgress(0, "Bucket Sort: анализ данных...");

        int maxValue = findMaxValue(inputFile);
        if (maxValue == -1) {
            updateStatus("Bucket Sort: файл не найден или пуст");
            return;
        }

        updateBucketProgress(20, "Bucket Sort: распределение по корзинам...");
        List<File> bucketFiles = distributeToBuckets(inputFile, maxValue);

        updateBucketProgress(50, "Bucket Sort: сортировка корзин...");
        sortAllBuckets(bucketFiles);

        updateBucketProgress(80, "Bucket Sort: объединение корзин...");
        mergeBucketsToFile(bucketFiles, outputFile);

        updateBucketProgress(95, "Bucket Sort: очистка временных файлов...");
        cleanupTempFiles(bucketFiles);

        updateBucketProgress(100, "Bucket Sort завершен!");
        updateBucketTimeLabel();
    }

    private int findMaxValue(String inputFile) throws IOException {
        File file = new File(inputFile);
        if (!file.exists()) {
            return -1;
        }

        int max = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    int value = Integer.parseInt(line.trim());
                    if (value > max) max = value;
                } catch (NumberFormatException e) {
                }
            }
        }
        return max;
    }

    private List<File> distributeToBuckets(String inputFile, int maxValue) throws IOException {
        final int BUCKET_COUNT = 32;
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

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                processedBytes += line.getBytes().length + 1;

                try {
                    int value = Integer.parseInt(line.trim());
                    int bucketIndex = (value * BUCKET_COUNT) / (maxValue + 1);
                    if (bucketIndex >= BUCKET_COUNT) bucketIndex = BUCKET_COUNT - 1;

                    writers.get(bucketIndex).write(value + "\n");
                } catch (NumberFormatException e) {
                }

                int progress = 20 + (int)((processedBytes * 30) / fileSize);
                updateBucketProgress(progress, "Bucket Sort: распределение " + processedBytes / 1024 + " КБ");
                updateBucketTimeLabel();
            }
        }

        for (BufferedWriter writer : writers) {
            writer.close();
        }

        return bucketFiles;
    }

    private void sortAllBuckets(List<File> bucketFiles) throws IOException {
        int totalBuckets = bucketFiles.size();
        for (int i = 0; i < totalBuckets; i++) {
            File bucketFile = bucketFiles.get(i);
            List<Integer> bucketData = readBucketToMemory(bucketFile);
            Collections.sort(bucketData);
            writeBucketToFile(bucketData, bucketFile);

            int progress = 50 + (i * 30) / totalBuckets;
            updateBucketProgress(progress, "Bucket Sort: сортировка корзины " + (i + 1) + "/" + totalBuckets);
            updateBucketTimeLabel();
        }
    }

    private List<Integer> readBucketToMemory(File bucketFile) throws IOException {
        List<Integer> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(bucketFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    data.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException e) {
                }
            }
        }
        return data;
    }

    private void writeBucketToFile(List<Integer> data, File bucketFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(bucketFile))) {
            for (int value : data) {
                writer.write(value + "\n");
            }
        }
    }

    private void mergeBucketsToFile(List<File> bucketFiles, String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            long totalElements = estimateTotalLines(bucketFiles);
            long processedElements = 0;

            for (File bucketFile : bucketFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(bucketFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        writer.write(line + "\n");
                        processedElements++;

                        if (processedElements % 10000 == 0) {
                            int progress = 80 + (int)((processedElements * 15) / totalElements);
                            updateBucketProgress(progress, "Bucket Sort: объединение " + processedElements + " элементов");
                            updateBucketTimeLabel();
                        }
                    }
                }
            }
        }
    }

    private long estimateTotalLines(List<File> files) throws IOException {
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

    private void cleanupTempFiles(List<File> tempFiles) {
        int deletedCount = 0;
        for (File file : tempFiles) {
            if (file.delete()) {
                deletedCount++;
            }
        }
    }

    private void updateBucketProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            bucketProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
        });
    }

    private void updateBucketProgress(int value) {
        updateBucketProgress(value, null);
    }

    private void updateBucketTimeLabel() {
        if (bucketStartTime > 0) {
            SwingUtilities.invokeLater(() -> {
                long elapsed = (System.currentTimeMillis() - bucketStartTime) / 1000;
                bucketTimeLabel.setText("Время: " + elapsed + " сек");
            });
        }
    }

    private void runOneWayMergeSort() throws IOException {
        updateStatus("One-Way: запуск...");
        String inputFile = "large_file.txt";
        String outputFile = "sorted_one_way.txt";

        updateOneWayProgress(0, "One-Way: разделение файла...");
        List<File> chunks = splitAndSortFiles(inputFile, "chunk1_", 0, 50, true);
        updateOneWayProgress(50, "One-Way: файл разделен на " + chunks.size() + " чанков");

        updateOneWayProgress(50, "One-Way: начало слияния...");
        oneWayMergeSortWithProgress(chunks, outputFile, 50, 50);

        cleanupTempFiles(chunks);
        updateOneWayProgress(100, "One-Way завершен!");
        updateOneWayTimeLabel();
    }

    private void runKWayMergeSort() throws IOException {
        updateStatus("K-Way: запуск...");
        String inputFile = "large_file.txt";
        String outputFile = "sorted_k_way.txt";

        updateKWayProgress(0, "K-Way: разделение файла...");
        List<File> chunks = splitAndSortFiles(inputFile, "chunk2_", 0, 50, false);
        updateKWayProgress(50, "K-Way: файл разделен на " + chunks.size() + " чанков");

        updateKWayProgress(50, "K-Way: начало многопутевого слияния...");
        kWayMergeSortWithProgress(chunks, outputFile, 50, 50);

        cleanupTempFiles(chunks);
        updateKWayProgress(100, "K-Way завершен!");
        updateKWayTimeLabel();
    }

    private void runReplacementSelectionSort() throws IOException {
        updateStatus("Replacement Selection: запуск...");
        String inputFile = "large_file.txt";
        String outputFile = "sorted_replacement.txt";

        updateReplacementProgress(0, "Replacement: создание серий...");
        List<File> series = replacementSelectionSort(inputFile, "replacement_");
        updateReplacementProgress(60, "Replacement: создано " + series.size() + " серий");

        updateReplacementProgress(60, "Replacement: слияние серий...");
        kWayMergeSortWithProgress(series, outputFile, 60, 40);

        cleanupTempFiles(series);
        updateReplacementProgress(100, "Replacement Selection завершен!");
        updateReplacementTimeLabel();
    }

    private List<File> replacementSelectionSort(String inputFile, String prefix) throws IOException {
        List<File> outputFiles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            PriorityQueue<String> currentRun = new PriorityQueue<>();
            PriorityQueue<String> nextRun = new PriorityQueue<>();
            List<String> currentOutput = new ArrayList<>();

            String lastOutput = null;
            long totalLines = 0;
            long processedLines = 0;

            try (BufferedReader countReader = new BufferedReader(new FileReader(inputFile))) {
                while (countReader.readLine() != null) {
                    totalLines++;
                }
            }

            for (int i = 0; i < REPLACEMENT_BUFFER_SIZE; i++) {
                String line = reader.readLine();
                if (line == null) break;
                currentRun.offer(line);
                processedLines++;
            }

            while (!currentRun.isEmpty() || !nextRun.isEmpty()) {
                if (currentRun.isEmpty()) {
                    if (!currentOutput.isEmpty()) {
                        File runFile = createSortedTempFile(currentOutput, prefix + outputFiles.size());
                        outputFiles.add(runFile);
                        currentOutput.clear();
                    }

                    PriorityQueue<String> temp = currentRun;
                    currentRun = nextRun;
                    nextRun = temp;
                    lastOutput = null;

                    updateReplacementProgress((int)((processedLines * 60) / totalLines),
                            "Replacement: начата серия " + (outputFiles.size() + 1));
                }

                String minElement = currentRun.poll();

                if (lastOutput == null || minElement.compareTo(lastOutput) >= 0) {
                    currentOutput.add(minElement);
                    lastOutput = minElement;

                    String nextLine = reader.readLine();
                    if (nextLine != null) {
                        processedLines++;
                        if (nextLine.compareTo(lastOutput) >= 0) {
                            currentRun.offer(nextLine);
                        } else {
                            nextRun.offer(nextLine);
                        }

                        if (processedLines % 1000 == 0) {
                            updateReplacementProgress((int)((processedLines * 60) / totalLines),
                                    "Replacement: обработано " + processedLines + "/" + totalLines + " строк");
                            updateReplacementTimeLabel();
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

    private List<File> splitAndSortFiles(String inputFile, String chunkPrefix,
                                         int progressStart, int progressRange, boolean isOneWay) throws IOException {
        File file = new File(inputFile);
        List<File> tempFiles = new ArrayList<>();

        if (!file.exists()) {
            updateStatus("ОШИБКА: Файл не найден - " + inputFile);
            return tempFiles;
        }

        long fileSize = file.length();
        long processedBytes = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> chunk = new ArrayList<>();
            String line;
            int count = 1;
            long currentChunkSize = 0;

            while ((line = reader.readLine()) != null) {
                int lineSize = getMemorySize(line);
                processedBytes += lineSize;

                int progress = progressStart + (int)((processedBytes * progressRange) / fileSize);
                if (isOneWay) {
                    updateOneWayProgress(progress);
                } else {
                    updateKWayProgress(progress);
                }

                if (currentChunkSize + lineSize <= MAX_MEMORY_SIZE) {
                    chunk.add(line);
                    currentChunkSize += lineSize;
                } else {
                    File tempFile = createSortedTempFile(chunk, chunkPrefix + count++);
                    tempFiles.add(tempFile);

                    chunk.clear();
                    chunk.add(line);
                    currentChunkSize = lineSize;

                    if (isOneWay) {
                        updateOneWayProgress(progress, "One-Way: создан чанк " + (count-1));
                    } else {
                        updateKWayProgress(progress, "K-Way: создан чанк " + (count-1));
                    }
                }

                updateTimeLabel();
                if (isOneWay) {
                    updateOneWayTimeLabel();
                } else {
                    updateKWayTimeLabel();
                }
            }

            if (!chunk.isEmpty()) {
                File tempFile = createSortedTempFile(chunk, chunkPrefix + count);
                tempFiles.add(tempFile);

                if (isOneWay) {
                    updateOneWayProgress(progressStart + progressRange, "One-Way: создан финальный чанк");
                } else {
                    updateKWayProgress(progressStart + progressRange, "K-Way: создан финальный чанк");
                }
            }
        }

        return tempFiles;
    }

    private void oneWayMergeSortWithProgress(List<File> chunks, String outputFile,
                                             int progressStart, int progressRange) throws IOException {
        if (chunks.isEmpty()) {
            return;
        }

        List<File> currentFiles = new ArrayList<>(chunks);
        int totalMerges = currentFiles.size() - 1;
        int completedMerges = 0;
        int stage = 1;

        while (currentFiles.size() > 1) {
            updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                    "One-Way: этап " + stage + " (файлов: " + currentFiles.size() + ")");

            List<File> mergedFiles = new ArrayList<>();
            for (int i = 0; i < currentFiles.size(); i += 2) {
                if (i + 1 < currentFiles.size()) {
                    File mergedFile = mergeTwoFiles(currentFiles.get(i), currentFiles.get(i + 1),
                            "one_way_temp_" + stage + "_" + i);
                    mergedFiles.add(mergedFile);
                    completedMerges++;
                } else {
                    mergedFiles.add(currentFiles.get(i));
                }

                updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                        "One-Way: слияние " + completedMerges + "/" + totalMerges);
                updateOneWayTimeLabel();
            }
            currentFiles = mergedFiles;
            stage++;
        }

        if (!currentFiles.isEmpty()) {
            Files.move(currentFiles.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void kWayMergeSortWithProgress(List<File> chunks, String outputFile,
                                           int progressStart, int progressRange) throws IOException {
        if (chunks.isEmpty()) {
            return;
        }

        if (chunks.size() == 1) {
            Files.copy(chunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            if (progressStart + progressRange <= 100) {
                updateKWayProgress(progressStart + progressRange);
            }
            return;
        }

        PriorityQueue<FileLine> pq = new PriorityQueue<>();
        List<BufferedReader> readers = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            for (File file : chunks) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                readers.add(reader);

                String firstLine = reader.readLine();
                if (firstLine != null) {
                    pq.offer(new FileLine(firstLine, reader));
                }
            }

            long totalLines = estimateTotalLines(chunks);
            long processedLines = 0;

            updateKWayProgress(progressStart, "K-Way: начало слияния " + chunks.size() + " чанков");

            while (!pq.isEmpty()) {
                FileLine minLine = pq.poll();
                writer.write(minLine.line);
                writer.newLine();
                processedLines++;

                if (processedLines % 10000 == 0) {
                    int progress = progressStart + (int)((processedLines * progressRange) / totalLines);
                    updateKWayProgress(progress,
                            "K-Way: обработано " + processedLines + "/" + totalLines + " строк");
                    updateKWayTimeLabel();
                }

                String nextLine = minLine.reader.readLine();
                if (nextLine != null) {
                    pq.offer(new FileLine(nextLine, minLine.reader));
                }
            }

            updateKWayProgress(progressStart + progressRange, "K-Way: слияние завершено");
        } finally {
            for (BufferedReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private File createSortedTempFile(List<String> chunk, String filename) throws IOException {
        Collections.sort(chunk, String.CASE_INSENSITIVE_ORDER);

        File tempFile = File.createTempFile(filename, ".tmp");
        tempFile.deleteOnExit();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String line : chunk) {
                writer.write(line);
                writer.newLine();
            }
        }

        return tempFile;
    }

    private File mergeTwoFiles(File file1, File file2, String tempName) throws IOException {
        File tempFile = File.createTempFile(tempName, ".tmp");
        tempFile.deleteOnExit();

        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1));
             BufferedReader reader2 = new BufferedReader(new FileReader(file2));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line1 = reader1.readLine();
            String line2 = reader2.readLine();

            while (line1 != null && line2 != null) {
                if (line1.compareTo(line2) <= 0) {
                    writer.write(line1);
                    writer.newLine();
                    line1 = reader1.readLine();
                } else {
                    writer.write(line2);
                    writer.newLine();
                    line2 = reader2.readLine();
                }
            }

            while (line1 != null) {
                writer.write(line1);
                writer.newLine();
                line1 = reader1.readLine();
            }

            while (line2 != null) {
                writer.write(line2);
                writer.newLine();
                line2 = reader2.readLine();
            }
        }

        file1.delete();
        file2.delete();

        return tempFile;
    }

    private int getMemorySize(String line) {
        return line.getBytes().length;
    }

    static class FileLine implements Comparable<FileLine> {
        String line;
        BufferedReader reader;

        FileLine(String line, BufferedReader reader) {
            this.line = line;
            this.reader = reader;
        }

        @Override
        public int compareTo(FileLine other) {
            return this.line.compareToIgnoreCase(other.line);
        }
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    private void updateTimeLabel() {
        SwingUtilities.invokeLater(() -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            timeLabel.setText("Общее время: " + elapsed + " сек");
        });
    }

    private void updateOneWayTimeLabel() {
        if (oneWayStartTime > 0) {
            SwingUtilities.invokeLater(() -> {
                long elapsed = (System.currentTimeMillis() - oneWayStartTime) / 1000;
                oneWayTimeLabel.setText("Время: " + elapsed + " сек");
            });
        }
    }

    private void updateKWayTimeLabel() {
        if (kWayStartTime > 0) {
            SwingUtilities.invokeLater(() -> {
                long elapsed = (System.currentTimeMillis() - kWayStartTime) / 1000;
                kWayTimeLabel.setText("Время: " + elapsed + " сек");
            });
        }
    }

    private void updateReplacementTimeLabel() {
        if (replacementStartTime > 0) {
            SwingUtilities.invokeLater(() -> {
                long elapsed = (System.currentTimeMillis() - replacementStartTime) / 1000;
                replacementTimeLabel.setText("Время: " + elapsed + " сек");
            });
        }
    }

    private void updateOneWayProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            oneWayProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
        });
    }

    private void updateOneWayProgress(int value) {
        updateOneWayProgress(value, null);
    }

    private void updateKWayProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            kWayProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
        });
    }

    private void updateKWayProgress(int value) {
        updateKWayProgress(value, null);
    }

    private void updateReplacementProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            replacementProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
        });
    }

    private void updateReplacementProgress(int value) {
        updateReplacementProgress(value, null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Coursework());
    }
}