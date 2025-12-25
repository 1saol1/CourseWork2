import java.awt.Color;
import java.io.IOException;

public interface ExternalSortAlgorithm {
    void sort(String inputFile, String outputFile, Coursework gui) throws IOException;
    // Выполняет сортировку файла и записывает результат в выходной файл

    String getAlgorithmName();
    // Возвращает читаемое название алгоритма для отображения в интерфейсе

    Color getProgressBarColor();
    // Возвращает цвет, который будет использоваться для прогресс-бара алгоритма

    String getAlgorithmId();
    // Возвращает уникальный текстовый идентификатор алгоритма
}