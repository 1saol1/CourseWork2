import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExternalMergeSorter<T extends Comparable<T>> {
    private final SortOptions options;
    private final InMemorySorter<T> inMemorySorter;

    public ExternalMergeSorter(SortOptions options, InMemorySorter<T> inMemorySorter) {
        this.options = options;
        this.inMemorySorter = inMemorySorter;
    }

    public void sortMultipleFiles() {
        options.validate();
        List<String> allSortedChunks = new ArrayList<>();

        try {
            for(String inputfile : options.getInputFilePaths()){
                List<String> fileChunks = sortSinglefile(inputfile);
                allSortedChunks.addAll(fileChunks);
            }
            mergeAllSortedChunks(allSortedChunks);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сортировки", e);
        }
    }
    private List<String> sortSinglefile(String inputFilePath){
        List<String> chunkFiles = new ArrayList<>();
        List<T> currentChunk = new ArrayList<>();
        long currentChunkSize = 0;
        int chunkCounter = 0;


        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))){
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null){
                lineCount++;

                if(line.trim().isEmpty()){
                    continue;
                }

                T value = convertLineToType(line);
                currentChunk.add(value);

                currentChunkSize += estimateSizeInMemory(line);

                if(currentChunkSize >= options.getMaxChunkSizeInBytes()){
                    String chunkFilePath = saveSortedChunk(currentChunk, chunkCounter++, inputFilePath);
                    chunkFiles.add(chunkFilePath);

                    currentChunk.clear();
                    currentChunkSize = 0;


                }

            }
            if(!currentChunk.isEmpty()){
                String chunkFilePath = saveSortedChunk(currentChunk, chunkCounter, inputFilePath);
                chunkFiles.add(chunkFilePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return chunkFiles;
    }

    private List<String> splitAndSortInitialFile() {


        return null;
    }
    private T convertLineToType(String line){
        return (T) line;
    }
    private long estimateSizeInMemory(String line){
        return line.length() * 2L + 64;
    }

    private String saveSortedChunk(List<T> chunk, int chunkNumber, String sourceFileName){
        return null;
    }

    private void mergeAllSortedChunks(List<String> chunkFiles) {
        System.out.println("Фаза 2: Слияние " + chunkFiles.size() + " чанков...");
        // TODO: Реализовать слияние чанков
    }

}