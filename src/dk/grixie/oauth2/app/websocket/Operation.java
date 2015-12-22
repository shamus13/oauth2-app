package dk.grixie.oauth2.app.websocket;

public enum Operation {
    TEXT(0x1), BINARY(0x2), CLOSE(0x8), PING(0x9), PONG(0xA);

    Operation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private final int code;
}
