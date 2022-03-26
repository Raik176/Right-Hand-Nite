package util;

public abstract class Manager {
    private Listener l;
    public Manager(Listener l) {
        listen(l);
    }
    public abstract void listen(Listener l);
}
