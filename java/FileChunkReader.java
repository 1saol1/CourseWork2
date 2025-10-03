public interface FileChunkReader<T extends Comparable<T>> extends AutoCloseable {
    boolean canRead();
    T readNextValue();
}
