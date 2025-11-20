import javax.swing.*;
import java.awt.*;

public class ExampleInterface extends JFrame {
    private JProgressBar oneWayProgressBar;
    private JProgressBar kWayProgressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel oneWayTimeLabel;
    private JLabel kWayTimeLabel;
    private JButton startButton;

    public ExampleInterface() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Сравнение сортировок - One-Way vs K-Way Merge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Главная панель с отступами
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Управление сортировкой"));

        startButton = new JButton("Начать сортировку");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(180, 35));
        controlPanel.add(startButton);

        // Панель прогресса (теперь 4 строки вместо 5)
        JPanel progressPanel = new JPanel(new GridLayout(4, 1, 8, 8));
        progressPanel.setBorder(BorderFactory.createTitledBorder("Прогресс выполнения"));

        // One-Way прогресс
        JPanel oneWayPanel = new JPanel(new BorderLayout(5, 5));
        oneWayPanel.add(new JLabel("One-Way Merge Sort:"), BorderLayout.NORTH);
        oneWayProgressBar = new JProgressBar(0, 100);
        oneWayProgressBar.setStringPainted(true);
        oneWayProgressBar.setForeground(new Color(0, 0, 150));
        oneWayProgressBar.setPreferredSize(new Dimension(400, 25));
        oneWayPanel.add(oneWayProgressBar, BorderLayout.CENTER);

        oneWayTimeLabel = new JLabel("Время: 0 сек");
        oneWayTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        oneWayPanel.add(oneWayTimeLabel, BorderLayout.SOUTH);

        // K-Way прогресс
        JPanel kWayPanel = new JPanel(new BorderLayout(5, 5));
        kWayPanel.add(new JLabel("K-Way Merge Sort:"), BorderLayout.NORTH);
        kWayProgressBar = new JProgressBar(0, 100);
        kWayProgressBar.setStringPainted(true);
        kWayProgressBar.setForeground(new Color(150, 0, 0));
        kWayProgressBar.setPreferredSize(new Dimension(400, 25));
        kWayPanel.add(kWayProgressBar, BorderLayout.CENTER);

        kWayTimeLabel = new JLabel("Время: 0 сек");
        kWayTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        kWayPanel.add(kWayTimeLabel, BorderLayout.SOUTH);

        // Информационная панель
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusLabel = new JLabel("Готов к работе");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        timeLabel = new JLabel("Общее время: 0 сек");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        infoPanel.add(statusLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(timeLabel);

        // Сборка интерфейса (без общего прогресса)
        progressPanel.add(oneWayPanel);
        progressPanel.add(kWayPanel);
        progressPanel.add(infoPanel);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(progressPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Обработчик кнопки (заглушка)
        startButton.addActionListener(e -> {
            // Здесь будет логика запуска сортировки
            startButton.setEnabled(false);
            statusLabel.setText("Сортировка запущена...");

            // Имитация работы (можно удалить)
            simulateProgress();
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Метод для имитации прогресса (можно удалить)
    private void simulateProgress() {
        final long startTime = System.currentTimeMillis();
        final boolean[] oneWayCompleted = {false};
        final boolean[] kWayCompleted = {false};

        Timer timer = new Timer(100, e -> {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - startTime) / 1000;

            int currentOneWay = oneWayProgressBar.getValue();
            int currentKWay = kWayProgressBar.getValue();

            // One-Way завершается быстрее (на 70% прогресса)
            if (!oneWayCompleted[0] && currentOneWay < 70) {
                oneWayProgressBar.setValue(currentOneWay + 2);
            } else if (!oneWayCompleted[0]) {
                oneWayCompleted[0] = true;
                oneWayProgressBar.setValue(100);
                oneWayTimeLabel.setText("Время: " + elapsedSeconds + " сек (завершено)");
            }

            // K-Way завершается медленнее
            if (!kWayCompleted[0] && currentKWay < 100) {
                kWayProgressBar.setValue(currentKWay + 1);
            } else if (!kWayCompleted[0]) {
                kWayCompleted[0] = true;
                kWayTimeLabel.setText("Время: " + elapsedSeconds + " сек (завершено)");
            }

            // Обновляем время для активных процессов
            if (!oneWayCompleted[0]) {
                oneWayTimeLabel.setText("Время: " + elapsedSeconds + " сек");
            }
            if (!kWayCompleted[0]) {
                kWayTimeLabel.setText("Время: " + elapsedSeconds + " сек");
            }
            timeLabel.setText("Общее время: " + elapsedSeconds + " сек");

            // Обновляем статус
            updateDetailedStatus(oneWayProgressBar.getValue(), kWayProgressBar.getValue(),
                    oneWayCompleted[0], kWayCompleted[0]);

            // Проверяем завершение обеих сортировок
            if (oneWayCompleted[0] && kWayCompleted[0]) {
                ((Timer)e.getSource()).stop();
                startButton.setEnabled(true);
                statusLabel.setText("Обе сортировки завершены!");
            }
        });
        timer.start();
    }

    private void updateDetailedStatus(int oneWayProgress, int kWayProgress,
                                      boolean oneWayDone, boolean kWayDone) {
        if (oneWayDone && kWayDone) {
            statusLabel.setText("Все операции завершены");
        } else if (oneWayDone) {
            statusLabel.setText("One-Way завершен, K-Way: " + kWayProgress + "%");
        } else if (kWayDone) {
            statusLabel.setText("K-Way завершен, One-Way: " + oneWayProgress + "%");
        } else if (oneWayProgress < 25 && kWayProgress < 25) {
            statusLabel.setText("Чтение и разделение файлов...");
        } else if (oneWayProgress < 50 && kWayProgress < 50) {
            statusLabel.setText("Внутренняя сортировка чанков...");
        } else if (oneWayProgress < 75 && kWayProgress < 75) {
            statusLabel.setText("Слияние отсортированных данных...");
        } else {
            statusLabel.setText("Финальная обработка...");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExampleInterface());
    }
}
