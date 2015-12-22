package dk.grixie.oauth2.app.websocket;

public class Frame {
    private final Operation operation;
    private final boolean last;
    private final byte [] data;

    public Frame(Operation operation, boolean last, byte[] data) {
        this.operation = operation;
        this.last = last;
        this.data = data;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isLast() {
        return last;
    }

    public byte[] getData() {
        return data;
    }
}
