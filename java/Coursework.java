import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Coursework extends JFrame {
    private JProgressBar oneWayProgressBar;
    private JProgressBar kWayProgressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel oneWayTimeLabel;
    private JLabel kWayTimeLabel;
    private JButton startButton;

    private long startTime;
    private long oneWayStartTime;
    private long kWayStartTime;
    private volatile boolean isRunning = false;

    private static final int MAX_MEMORY_SIZE = 250 * 1024 * 1024;

    public Coursework() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Сравнение сортировок - One-Way vs K-Way Merge");
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


        JPanel progressPanel = new JPanel(new GridLayout(4, 1, 8, 8));
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
        oneWayTimeLabel.setText("Время: 0 сек");
        kWayTimeLabel.setText("Время: 0 сек");


        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                startTime = System.currentTimeMillis();
                oneWayStartTime = 0;
                kWayStartTime = 0;

                updateStatus("Запуск обеих сортировок...");
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

                oneWayThread.start();
                kWayThread.start();


                oneWayThread.join();
                kWayThread.join();

                return null;
            }

            @Override
            protected void done() {
                isRunning = false;
                startButton.setEnabled(true);
                updateStatus("Обе сортировки завершены!");
                updateTimeLabel();
            }
        };
        worker.execute();
    }

    private void runOneWayMergeSort() throws IOException {
        updateStatus("One-Way: запуск...");
        String inputFile1 = "large_file1.txt";
        String inputFile2 = "large_file2.txt";
        String outputFile = "sorted_one_way.txt";


        updateOneWayProgress(0, "Разделение файлов...");
        List<File> chunks1 = splitAndSortFilesOneWay(inputFile1, "chunk1_", 0, 40);
        updateOneWayProgress(20, "Первый файл разделен");

        List<File> chunks2 = splitAndSortFilesOneWay(inputFile2, "chunk2_", 20, 20);
        updateOneWayProgress(40, "Второй файл разделен");


        updateOneWayProgress(40, "Начало слияния...");
        oneWayMergeSortWithProgress(chunks1, chunks2, outputFile, 40, 60);

        cleanupTempFiles(chunks1);
        cleanupTempFiles(chunks2);

        updateOneWayProgress(100, "One-Way завершен!");
        updateOneWayTimeLabel();
    }

    private void runKWayMergeSort() throws IOException {
        updateStatus("K-Way: запуск...");
        String inputFile1 = "large_file1.txt";
        String inputFile2 = "large_file2.txt";
        String outputFile = "sorted_k_way.txt";


        updateKWayProgress(0, "Разделение файлов...");
        List<File> chunks1 = splitAndSortFilesKWay(inputFile1, "chunk3_", 0, 40);
        updateKWayProgress(20, "Первый файл разделен");

        List<File> chunks2 = splitAndSortFilesKWay(inputFile2, "chunk4_", 20, 20);
        updateKWayProgress(40, "Второй файл разделен");


        updateKWayProgress(40, "Начало K-Way слияния...");
        kWayMergeSortWithProgress(chunks1, chunks2, outputFile, 40, 60);

        cleanupTempFiles(chunks1);
        cleanupTempFiles(chunks2);

        updateKWayProgress(100, "K-Way завершен!");
        updateKWayTimeLabel();
    }

    private List<File> splitAndSortFilesOneWay(String inputFile, String chunkPrefix,
                                               int progressStart, int progressRange) throws IOException {
        return splitAndSortFiles(inputFile, chunkPrefix, progressStart, progressRange, true);
    }

    private List<File> splitAndSortFilesKWay(String inputFile, String chunkPrefix,
                                             int progressStart, int progressRange) throws IOException {
        return splitAndSortFiles(inputFile, chunkPrefix, progressStart, progressRange, false);
    }

    private List<File> splitAndSortFiles(String inputFile, String chunkPrefix,
                                         int progressStart, int progressRange, boolean isOneWay) throws IOException {
        File file = new File(inputFile);
        List<File> tempFiles = new ArrayList<>();
        if (!file.exists()) {
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
            }
        }

        return tempFiles;
    }

    private void oneWayMergeSortWithProgress(List<File> chunks1, List<File> chunks2,
                                             String outputFile, int progressStart, int progressRange) throws IOException {
        List<File> allChunks = new ArrayList<>();
        allChunks.addAll(chunks1);
        allChunks.addAll(chunks2);

        int totalMerges = allChunks.size() - 1;
        int completedMerges = 0;
        int stage = 1;

        while (allChunks.size() > 1) {
            updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                    "One-Way: этап " + stage);

            List<File> mergedChunks = new ArrayList<>();
            for (int i = 0; i < allChunks.size(); i += 2) {
                if (i + 1 < allChunks.size()) {
                    File mergedFile = mergeTwoFiles(allChunks.get(i), allChunks.get(i + 1),
                            "one_way_temp_" + System.currentTimeMillis());
                    mergedChunks.add(mergedFile);
                    completedMerges++;
                } else {
                    mergedChunks.add(allChunks.get(i));
                }

                updateOneWayProgress(progressStart + (completedMerges * progressRange) / totalMerges,
                        "One-Way: слияние " + completedMerges + "/" + totalMerges);
                updateOneWayTimeLabel();
            }
            allChunks = mergedChunks;
            stage++;
        }

        if (!allChunks.isEmpty()) {
            Files.move(allChunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void kWayMergeSortWithProgress(List<File> chunks1, List<File> chunks2,
                                           String outputFile, int progressStart, int progressRange) throws IOException {
        List<File> allChunks = new ArrayList<>();
        allChunks.addAll(chunks1);
        allChunks.addAll(chunks2);

        if (allChunks.size() == 1) {
            Files.copy(allChunks.get(0).toPath(), Paths.get(outputFile), StandardCopyOption.REPLACE_EXISTING);
            updateKWayProgress(100);
            return;
        }

        PriorityQueue<FileLine> pq = new PriorityQueue<>();
        List<BufferedReader> readers = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {

            for (File file : allChunks) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                readers.add(reader);
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    pq.offer(new FileLine(firstLine, reader));
                }
            }

            long totalLines = estimateTotalLines(allChunks);
            long processedLines = 0;


            while (!pq.isEmpty()) {
                FileLine minLine = pq.poll();
                writer.write(minLine.line);
                writer.newLine();
                processedLines++;


                if (processedLines % 10000 == 0) {
                    int progress = progressStart + (int)((processedLines * progressRange) / totalLines);
                    updateKWayProgress(progress);
                    updateKWayTimeLabel();
                }

                String nextLine = minLine.reader.readLine();
                if (nextLine != null) {
                    pq.offer(new FileLine(nextLine, minLine.reader));
                }
            }
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
        for (File file : tempFiles) {
            file.delete();
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Coursework());
    }
}
