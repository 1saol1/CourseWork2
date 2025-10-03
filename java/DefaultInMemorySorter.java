import java.util.List;
import java.util.Collections;

public class DefaultInMemorySorter<T extends Comparable<T>> implements InMemorySorter<T> {

    @Override
    public void sort(List<T> data) {
        Collections.sort(data);
    }
}