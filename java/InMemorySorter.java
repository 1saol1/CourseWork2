import java.util.List;

public interface InMemorySorter<T extends Comparable<T>> {
    void sort(List<T> data);
}