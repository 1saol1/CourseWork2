import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Coursework extends JFrame {
    private JProgressBar oneWayProgressBar;
    private JProgressBar kWayProgressBar;
    private JProgressBar replacementProgressBar;
    private JProgressBar bucketProgressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel totalTimeLabel;
    private JLabel oneWayTimeLabel;
    private JLabel kWayTimeLabel;
    private JLabel replacementTimeLabel;
    private JLabel bucketTimeLabel;
    private JButton startButton;
    private JButton selectButton;
    private JButton modeButton;
    private JTextField pathField;
    private String selectedFilePath;

    private long startTime;
    private long totalSortingTime;
    private volatile boolean isRunning = false;
    private boolean parallelMode = true;

    // Хранит время выполнения каждого алгоритма
    private long oneWayTime = 0;
    private long kWayTime = 0;
    private long replacementTime = 0;
    private long bucketTime = 0;

    // Список доступных алгоритмов сортировки
    private List<ExternalSortAlgorithm> sortingAlgorithms;

    public Coursework() {
        // Инициализирует алгоритмы сортировки
        initializeAlgorithms();
        // Создает графический интерфейс
        initializeGUI();
    }

    private void initializeAlgorithms() {
        // Создает список алгоритмов для сортировки
        sortingAlgorithms = Arrays.asList(
                new OneWayMergeSort(),
                new KWayMergeSort(),
                new ReplacementSelectionSort(),
                new BucketSort()
        );
    }

    private void initializeGUI() {
        // Настраивает основные параметры окна
        setTitle("OutSorts - Сравнение алгоритмов внешней сортировки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(1200, 900));
        setResizable(true);

        // Создает главную панель с отступами
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(57, 57, 57));

        // Создает панель для выбора файла
        JPanel fileSelectionPanel = createFileSelectionPanel();
        mainPanel.add(fileSelectionPanel, BorderLayout.NORTH);

        // Создает панель с управлением и прогрессом
        JPanel controlProgressPanel = createControlProgressPanel();
        mainPanel.add(controlProgressPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Настраивает размер и положение окна
        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private JPanel createFileSelectionPanel() {
        // Создает панель с элементами для выбора файла
        JPanel filePanel = new JPanel(new BorderLayout(15, 15));
        filePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(89, 13, 51), 2),
                        "Выбор файла для сортировки"),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        filePanel.setBackground(Color.WHITE);
        filePanel.setPreferredSize(new Dimension(1150, 120));

        // Создает панель с полем ввода и кнопками
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBackground(Color.WHITE);

        // Создает поле для отображения пути к файлу
        pathField = new JTextField(60);
        pathField.setEditable(false);
        pathField.setFont(new Font("Arial", Font.PLAIN, 14));
        pathField.setPreferredSize(new Dimension(800, 35));
        pathField.setBackground(new Color(250, 250, 250));
        pathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Создает кнопку для выбора файла
        selectButton = new JButton("Выбрать файл");
        selectButton.setFont(new Font("Arial", Font.BOLD, 14));
        selectButton.setPreferredSize(new Dimension(180, 35));
        selectButton.setBackground(new Color(89, 13, 51));
        selectButton.setForeground(Color.WHITE);
        selectButton.setFocusPainted(false);

        // Создает кнопку для переключения режима работы
        modeButton = new JButton("Режим: ПАРАЛЛЕЛЬНЫЙ");
        modeButton.setFont(new Font("Arial", Font.BOLD, 12));
        modeButton.setPreferredSize(new Dimension(220, 35));
        modeButton.setBackground(new Color(13, 51, 89));
        modeButton.setForeground(Color.WHITE);
        modeButton.setFocusPainted(false);

        // Создает кнопку для запуска сортировки
        startButton = new JButton("Начать сортировки");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(200, 35));
        startButton.setBackground(new Color(89, 13, 51));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setEnabled(false);

        // Добавляет элементы на панель
        controlPanel.add(new JLabel("Файл:"));
        controlPanel.add(pathField);
        controlPanel.add(selectButton);
        controlPanel.add(modeButton);
        controlPanel.add(startButton);

        filePanel.add(controlPanel, BorderLayout.CENTER);

        // Назначает обработчики событий для кнопок
        selectButton.addActionListener(e -> selectFile());
        startButton.addActionListener(e -> startSorting());
        modeButton.addActionListener(e -> toggleSortingMode());

        return filePanel;
    }

    private JPanel createControlProgressPanel() {
        // Создает главную панель для отображения прогресса
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(57, 57, 57));

        // Создает панель с информацией о статусе
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(57, 57, 57)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Создает метку для отображения статуса
        statusLabel = new JLabel("Выберите файл для начала сортировки");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(new Color(70, 70, 70));

        // Создает метку для отображения текущего времени
        timeLabel = new JLabel("Время: 0 сек");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        timeLabel.setForeground(new Color(100, 100, 100));

        // Создает метку для отображения общего времени
        totalTimeLabel = new JLabel("Общее время: не измерено");
        totalTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalTimeLabel.setForeground(new Color(89, 13, 51));

        // Добавляет элементы на информационную панель
        infoPanel.add(statusLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(timeLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(totalTimeLabel);

        // Создает панель с прогресс-барами
        JPanel progressPanel = new JPanel(new GridLayout(4, 1, 15, 15));
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
                        "Прогресс выполнения алгоритмов"),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        progressPanel.setBackground(Color.WHITE);
        progressPanel.setPreferredSize(new Dimension(1150, 500));

        // Создает панель для One-Way Merge Sort
        JPanel oneWayPanel = createAlgorithmPanel("One-Way Merge Sort:",
                oneWayProgressBar = createProgressBar(new Color(0, 0, 150)),
                oneWayTimeLabel = createTimeLabel());
        progressPanel.add(oneWayPanel);

        // Создает панель для K-Way Merge Sort
        JPanel kWayPanel = createAlgorithmPanel("K-Way Merge Sort:",
                kWayProgressBar = createProgressBar(new Color(150, 0, 0)),
                kWayTimeLabel = createTimeLabel());
        progressPanel.add(kWayPanel);

        // Создает панель для Replacement Selection Sort
        JPanel replacementPanel = createAlgorithmPanel("Replacement Selection Sort:",
                replacementProgressBar = createProgressBar(new Color(0, 100, 0)),
                replacementTimeLabel = createTimeLabel());
        progressPanel.add(replacementPanel);

        // Создает панель для Bucket Sort
        JPanel bucketPanel = createAlgorithmPanel("Bucket Sort:",
                bucketProgressBar = createProgressBar(new Color(150, 0, 150)),
                bucketTimeLabel = createTimeLabel());
        progressPanel.add(bucketPanel);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(progressPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAlgorithmPanel(String title, JProgressBar progressBar, JLabel timeLabel) {
        // Создает панель для отображения прогресса одного алгоритма
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 60, 60));

        progressBar.setPreferredSize(new Dimension(900, 25));

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timePanel.setBackground(Color.WHITE);
        timePanel.add(timeLabel);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(timePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JProgressBar createProgressBar(Color color) {
        // Создает и настраивает прогресс-бар с заданным цветом
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(color);
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        progressBar.setFont(new Font("Arial", Font.PLAIN, 11));
        return progressBar;
    }

    private JLabel createTimeLabel() {
        // Создает метку для отображения времени выполнения алгоритма
        JLabel label = new JLabel("Время: не измерено");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(new Color(100, 100, 100));
        return label;
    }

    private void toggleSortingMode() {
        // Переключает между параллельным и последовательным режимами
        parallelMode = !parallelMode;
        if (parallelMode) {
            modeButton.setText("Режим: ПАРАЛЛЕЛЬНЫЙ");
            modeButton.setBackground(new Color(13, 51, 89));
        } else {
            modeButton.setText("Режим: ПОСЛЕДОВАТЕЛЬНЫЙ");
            modeButton.setBackground(new Color(89, 51, 13));
        }
        updateStatus("Режим изменен на: " + (parallelMode ? "ПАРАЛЛЕЛЬНЫЙ" : "ПОСЛЕДОВАТЕЛЬНЫЙ"));
    }

    private void selectFile() {
        // Открывает диалог выбора файла и сохраняет выбранный путь
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файл для сортировки");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedFilePath = selectedFile.getAbsolutePath();
            pathField.setText(selectedFile.getAbsolutePath());
            startButton.setEnabled(true);
            updateStatus("Файл выбран: " + selectedFile.getName() + " - готов к сортировке");

            setTitle("OutSorts - " + selectedFile.getName());
        }
    }

    private String getOutputFilePath(String suffix) {
        // Генерирует путь для выходного файла на основе входного
        if (selectedFilePath == null) {
            return "sorted_" + suffix + ".txt";
        }

        File inputFile = new File(selectedFilePath);
        String parentDir = inputFile.getParent();
        String fileName = inputFile.getName();
        String baseName = fileName.contains(".") ?
                fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        return parentDir + File.separator + baseName + "_sorted_" + suffix + ".txt";
    }

    private void startSorting() {
        // Запускает процесс сортировки выбранного файла
        if (isRunning || selectedFilePath == null) return;

        startButton.setEnabled(false);
        selectButton.setEnabled(false);
        modeButton.setEnabled(false);
        isRunning = true;

        // Сбрасывает все прогресс-бары и метки
        resetProgressBars();

        // Обнуляет время выполнения алгоритмов
        oneWayTime = 0;
        kWayTime = 0;
        replacementTime = 0;
        bucketTime = 0;
        totalSortingTime = 0;
        totalTimeLabel.setText("Общее время: не измерено");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Запускает сортировку в отдельном потоке
                startTime = System.currentTimeMillis();
                updateStatus("Запуск сортировок...");

                if (parallelMode) {
                    runParallelSorting();
                } else {
                    runSequentialSorting();
                }

                return null;
            }

            @Override
            protected void done() {
                // Выполняется после завершения сортировки
                long endTime = System.currentTimeMillis();
                totalSortingTime = endTime - startTime;

                isRunning = false;
                startButton.setEnabled(true);
                selectButton.setEnabled(true);
                modeButton.setEnabled(true);
                updateStatus("Все сортировки завершены!");

                // Обновляет отображение общего времени
                totalTimeLabel.setText(String.format("Общее время: %.2f сек", totalSortingTime / 1000.0));

                // Показывает результаты сортировки
                showResults();
            }
        };
        worker.execute();
    }

    private void runParallelSorting() throws Exception {
        // Запускает все алгоритмы сортировки параллельно
        updateStatus("Запуск ВСЕХ сортировок ПАРАЛЛЕЛЬНО...");

        ExecutorService executor = Executors.newFixedThreadPool(sortingAlgorithms.size());
        List<Future<Long>> futures = new ArrayList<>();

        // Создает задачи для каждого алгоритма
        for (ExternalSortAlgorithm algorithm : sortingAlgorithms) {
            Callable<Long> task = () -> {
                long start = System.currentTimeMillis();
                algorithm.sort(selectedFilePath, getOutputFilePath(algorithm.getAlgorithmId()), Coursework.this);
                return System.currentTimeMillis() - start;
            };
            futures.add(executor.submit(task));
        }

        // Собирает результаты выполнения алгоритмов
        for (int i = 0; i < sortingAlgorithms.size(); i++) {
            try {
                long executionTime = futures.get(i).get();
                updateAlgorithmTime(sortingAlgorithms.get(i), executionTime);
            } catch (Exception e) {
                updateStatus("Ошибка при выполнении " + sortingAlgorithms.get(i).getAlgorithmName() + ": " + e.getMessage());
            }
        }

        executor.shutdown();
    }

    private void runSequentialSorting() throws Exception {
        // Запускает алгоритмы сортировки последовательно один за другим
        updateStatus("Запуск сортировок ПОСЛЕДОВАТЕЛЬНО...");

        for (ExternalSortAlgorithm algorithm : sortingAlgorithms) {
            updateStatus("Запуск " + algorithm.getAlgorithmName() + "...");
            long startTime = System.currentTimeMillis();

            // Выполняет сортировку текущим алгоритмом
            algorithm.sort(selectedFilePath, getOutputFilePath(algorithm.getAlgorithmId()), this);

            long endTime = System.currentTimeMillis();
            updateAlgorithmTime(algorithm, endTime - startTime);
        }
    }

    private void updateAlgorithmTime(ExternalSortAlgorithm algorithm, long time) {
        // Обновляет время выполнения для конкретного алгоритма
        switch (algorithm.getAlgorithmId()) {
            case "one_way":
                oneWayTime = time;
                updateOneWayTimeLabel();
                break;
            case "k_way":
                kWayTime = time;
                updateKWayTimeLabel();
                break;
            case "replacement":
                replacementTime = time;
                updateReplacementTimeLabel();
                break;
            case "bucket":
                bucketTime = time;
                updateBucketTimeLabel();
                break;
        }
    }

    private void showResults() {
        // Отображает диалоговое окно с результатами сортировки
        String results = String.format(
                "Все сортировки успешно завершены!\n\n" +
                        "Общее время работы: %.2f секунд\n" +
                        "Режим: %s\n\n" +
                        "Время работы алгоритмов:\n" +
                        "1. One-Way Merge Sort: %.2f сек\n" +
                        "2. K-Way Merge Sort: %.2f сек\n" +
                        "3. Replacement Selection Sort: %.2f сек\n" +
                        "4. Bucket Sort: %.2f сек\n\n" +
                        "Результаты сохранены в той же директории, что и исходный файл.",
                totalSortingTime / 1000.0,
                parallelMode ? "ПАРАЛЛЕЛЬНЫЙ" : "ПОСЛЕДОВАТЕЛЬНЫЙ",
                oneWayTime / 1000.0,
                kWayTime / 1000.0,
                replacementTime / 1000.0,
                bucketTime / 1000.0
        );

        JOptionPane.showMessageDialog(Coursework.this,
                results,
                "Сортировка завершена",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetProgressBars() {
        // Сбрасывает значения всех прогресс-баров и меток времени
        oneWayProgressBar.setValue(0);
        kWayProgressBar.setValue(0);
        replacementProgressBar.setValue(0);
        bucketProgressBar.setValue(0);
        oneWayTimeLabel.setText("Время: не измерено");
        kWayTimeLabel.setText("Время: не измерено");
        replacementTimeLabel.setText("Время: не измерено");
        bucketTimeLabel.setText("Время: не измерено");
    }

    public void updateStatus(String status) {
        // Обновляет текстовый статус в GUI
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    public void updateTimeLabel() {
        // Обновляет метку текущего времени выполнения
        SwingUtilities.invokeLater(() -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            timeLabel.setText("Время: " + elapsed + " сек");
        });
    }

    public void updateOneWayTimeLabel() {
        // Обновляет метку времени для One-Way Merge Sort
        SwingUtilities.invokeLater(() -> {
            oneWayTimeLabel.setText(String.format("Время: %.2f сек", oneWayTime / 1000.0));
        });
    }

    public void updateKWayTimeLabel() {
        // Обновляет метку времени для K-Way Merge Sort
        SwingUtilities.invokeLater(() -> {
            kWayTimeLabel.setText(String.format("Время: %.2f сек", kWayTime / 1000.0));
        });
    }

    public void updateReplacementTimeLabel() {
        // Обновляет метку времени для Replacement Selection Sort
        SwingUtilities.invokeLater(() -> {
            replacementTimeLabel.setText(String.format("Время: %.2f сек", replacementTime / 1000.0));
        });
    }

    public void updateBucketTimeLabel() {
        // Обновляет метку времени для Bucket Sort
        SwingUtilities.invokeLater(() -> {
            bucketTimeLabel.setText(String.format("Время: %.2f сек", bucketTime / 1000.0));
        });
    }

    public void updateOneWayProgress(int value, String status) {
        // Обновляет прогресс-бар и статус для One-Way Merge Sort
        SwingUtilities.invokeLater(() -> {
            oneWayProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
            updateTimeLabel();
        });
    }

    public void updateOneWayProgress(int value) {
        // Обновляет только прогресс-бар для One-Way Merge Sort
        updateOneWayProgress(value, null);
    }

    public void updateKWayProgress(int value, String status) {
        // Обновляет прогресс-бар и статус для K-Way Merge Sort
        SwingUtilities.invokeLater(() -> {
            kWayProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
            updateTimeLabel();
        });
    }

    public void updateKWayProgress(int value) {
        // Обновляет только прогресс-бар для K-Way Merge Sort
        updateKWayProgress(value, null);
    }

    public void updateReplacementProgress(int value, String status) {
        // Обновляет прогресс-бар и статус для Replacement Selection Sort
        SwingUtilities.invokeLater(() -> {
            replacementProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
            updateTimeLabel();
        });
    }

    public void updateReplacementProgress(int value) {
        // Обновляет только прогресс-бар для Replacement Selection Sort
        updateReplacementProgress(value, null);
    }

    public void updateBucketProgress(int value, String status) {
        // Обновляет прогресс-бар и статус для Bucket Sort
        SwingUtilities.invokeLater(() -> {
            bucketProgressBar.setValue(value);
            if (status != null) {
                updateStatus(status);
            }
            updateTimeLabel();
        });
    }

    public void updateBucketProgress(int value) {
        // Обновляет только прогресс-бар для Bucket Sort
        updateBucketProgress(value, null);
    }

    public static void main(String[] args) {
        // Проверяет настройки памяти и запускает приложение
        checkMemorySettings();

        SwingUtilities.invokeLater(() -> new Coursework());
    }

    private static void checkMemorySettings() {
        // Проверяет доступную память и выводит предупреждение при недостатке
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);

        System.out.println("=== Memory Settings ===");
        System.out.println("Max memory: " + maxMemory + " MB");
        System.out.println("Total memory: " + totalMemory + " MB");

        if (maxMemory < 3500) {
            System.err.println("ВНИМАНИЕ: Мало памяти! Запустите с: java -Xmx4g Coursework");
        }
    }
}