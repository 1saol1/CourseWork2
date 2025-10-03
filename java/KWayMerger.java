import java.util.List;

public interface KWayMerger<T extends Comparable<T>> {
    Iterable<T> merge(List<FileChunkReader<T>> chunkReaders);
}