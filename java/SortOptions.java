import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class SortOptions {
    private List<String> inputFilePaths = new ArrayList<>(); // Список входных файлов
    private String outputFilePath; // Единый выходной файл
    private long maxChunkSizeInBytes = 100 * 1024 * 1024; // 100 MB
    private String tempFileDirectory = System.getProperty("java.io.tmpdir");
    private int fileBufferSizeInBytes = 4 * 1024 * 1024; // 4 MB
    private Charset fileEncoding = StandardCharsets.UTF_8;

    // Конструкторы
    public SortOptions() {}

    public SortOptions(List<String> inputFilePaths, String outputFilePath) {
        this.inputFilePaths = inputFilePaths;
        this.outputFilePath = outputFilePath;
    }

    // Геттеры и сеттеры
    public List<String> getInputFilePaths() {
        return inputFilePaths;
    }

    public void setInputFilePaths(List<String> inputFilePaths) {
        this.inputFilePaths = inputFilePaths;
    }

    public void addInputFilePath(String filePath) {
        this.inputFilePaths.add(filePath);
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public long getMaxChunkSizeInBytes() {
        return maxChunkSizeInBytes;
    }

    public void setMaxChunkSizeInBytes(long maxChunkSizeInBytes) {
        this.maxChunkSizeInBytes = maxChunkSizeInBytes;
    }

    public String getTempFileDirectory() {
        return tempFileDirectory;
    }

    public void setTempFileDirectory(String tempFileDirectory) {
        this.tempFileDirectory = tempFileDirectory;
    }

    public int getFileBufferSizeInBytes() {
        return fileBufferSizeInBytes;
    }

    public void setFileBufferSizeInBytes(int fileBufferSizeInBytes) {
        this.fileBufferSizeInBytes = fileBufferSizeInBytes;
    }

    public Charset getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(Charset fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    // Метод валидации для нескольких файлов
    public void validate() {
        if (inputFilePaths == null || inputFilePaths.isEmpty()) {
            throw new IllegalArgumentException("Input file paths are required");
        }

        for (String filePath : inputFilePaths) {
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("Input file path cannot be empty");
            }

            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                throw new RuntimeException("Input file not found: " + filePath);
            }
        }

        if (outputFilePath == null || outputFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output file path is required");
        }
    }
}