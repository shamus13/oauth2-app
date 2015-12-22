package dk.grixie.oauth2.app.model;

import android.os.Messenger;

public interface OAuth2Worker {
    public static enum ConnectionType {
        NONE, SLOW, FAST
    }

    Messenger getWorkerMessenger();

    ConnectionType getNetWorkStatus();

    public void release();
}
