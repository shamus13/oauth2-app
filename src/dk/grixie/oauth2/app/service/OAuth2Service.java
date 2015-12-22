package dk.grixie.oauth2.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import dk.grixie.oauth2.app.model.OAuth2WorkerProxy;

public class OAuth2Service extends Service {

    private OAuth2WorkerProxy proxy = null;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        proxy = new OAuth2WorkerProxy(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        proxy.release();
        proxy = null;
    }
}
