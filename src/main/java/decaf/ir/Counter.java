package decaf.ir;

public class Counter {
    private int nextId = 0;

    private static Counter instance = null;

    public static Counter getInstance() {
        if (instance == null) {
            instance = new Counter();
        }
        return instance;
    }

    public int nextId() {
        return nextId++;
    }
}
