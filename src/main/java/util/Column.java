package util;

public class Column<E,S,U> {
    private E entry;
    private S key;
    private U value;
    public Column(E entry, S key, U value) {
        this.entry = entry;
        this.key = key;
        this.value = value;
    }

    public E getEntry() {
        return entry;
    }

    public S getKey() {
        return key;
    }

    public U getValue() {
        return value;
    }
}
