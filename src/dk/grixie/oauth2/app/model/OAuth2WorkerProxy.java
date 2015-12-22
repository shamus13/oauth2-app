package dk.grixie.oauth2.app.model;

import android.content.Context;
import android.os.Messenger;

public class OAuth2WorkerProxy implements OAuth2Worker {

    private static OAuth2Worker instance = null;
    private static int count = 0;

    public OAuth2WorkerProxy(Context context) {
        synchronized (getClass()) {
            if (instance == null) {
                instance = new OAuth2WorkerImpl(context);
            }

            ++count;
        }
    }

    @Override
    public Messenger getWorkerMessenger() {
        return instance.getWorkerMessenger();
    }

    @Override
    public ConnectionType getNetWorkStatus() {
        return instance.getNetWorkStatus();
    }

    @Override
    public void release() {
        synchronized (getClass()) {
            --count;

            if (count == 0) {
                instance.release();

                instance = null;
            }
        }
    }
}
