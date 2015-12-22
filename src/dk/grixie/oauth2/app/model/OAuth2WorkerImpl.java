package dk.grixie.oauth2.app.model;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import dk.grixie.oauth2.app.Configuration;
import dk.grixie.oauth2.app.R;
import dk.grixie.oauth2.app.oauth2.AuthorizationServerProxy;
import dk.grixie.oauth2.app.oauth2.ResourceServerProxy;
import dk.grixie.oauth2.app.oauth2.exception.InvalidGrantException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidTokenException;
import dk.grixie.oauth2.app.oauth2.exception.OAuth2Exception;
import dk.grixie.oauth2.app.oauth2.token.AccessToken;
import dk.grixie.oauth2.app.oauth2.token.AuthorizationCodeGrant;
import dk.grixie.oauth2.app.oauth2.token.AuthorizationRequest;
import dk.grixie.oauth2.app.websocket.Frame;
import dk.grixie.oauth2.app.websocket.Operation;
import dk.grixie.oauth2.app.websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

public class OAuth2WorkerImpl implements OAuth2Worker, Observer {

    private static final String CLIENT_ID = "mobile";
    private static final String CLIENT_PASSWORD = "password";
    private static final String[] SCOPES = {"USER_ID", "USER_NAME"};

    private Context applicationContext = null;
    private Configuration configuration = null;

    private SSLContext sslContext = null;

    private AuthorizationRequest authorizationRequest = null;
    private AccessToken accessToken = null;
    private WebSocket webSocket = null;

    private Handler worker = null;
    private Messenger workerMessenger = null;
    private Messenger mainMessenger = null;

    private AuthorizationServerProxy authorizationServerProxy = null;
    private ResourceServerProxy resourceServerProxy = null;

    private Uri timeServiceEndPointUri = null;
    private Uri resourceOwnerServiceEndPointUri = null;
    private Uri messageServiceEndPointUri = null;
    private Uri echoWebSocketUri = null;

    public enum Type {ACCESS_TOKEN, AUTHORIZATION_REQUEST}

    private final class MainHandler extends Handler {

        private MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.arg1 == Type.ACCESS_TOKEN.ordinal()) {
                configuration.setAccessToken((AccessToken) message.obj);
            } else if (message.arg1 == Type.AUTHORIZATION_REQUEST.ordinal()) {
                configuration.setAuthorizationRequest((AuthorizationRequest) message.obj);
            }
        }
    }

    private final class OAuth2WorkerHandler extends Handler {
        private OAuth2WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            final Messenger replyTo = message.replyTo;

            if (message.arg1 == MessageType.REFRESH_TOKEN.ordinal()) {
                Message update = Message.obtain();
                update.arg1 = Type.ACCESS_TOKEN.ordinal();

                try {
                    AccessToken at = authorizationServerProxy.refreshAccessToken(getAccessToken());

                    setAccessToken(at);

                    update.obj = at;
                } catch (Exception e) {
                    update.obj = null;

                    setAccessToken(null);
                }

                sendResponse(mainMessenger, update);
            } else if (message.arg1 == MessageType.AUTHORIZATION_CODE_GRANT_REQUEST.ordinal()) {
                Message update;

                try {
                    AuthorizationRequest ar = buildAuthorizationRequest();
                    setAuthorizationRequest(ar);

                    update = Message.obtain();
                    update.arg1 = Type.AUTHORIZATION_REQUEST.ordinal();
                    update.obj = ar;

                    sendResponse(mainMessenger, update);

                    Intent intent = new Intent("android.intent.action.VIEW", buildAuthorizationCodeGrantRequestUri());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    applicationContext.startActivity(intent);
                } catch (final Exception e) {
                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.AUTHORIZATION_CODE_GRANT_RESPONSE.ordinal();
                    temp.obj = e;

                    sendResponse(replyTo, temp);
                }

                setAccessToken(null);

                update = Message.obtain();
                update.arg1 = Type.ACCESS_TOKEN.ordinal();
                update.obj = null;

                sendResponse(mainMessenger, update);
            } else if (message.arg1 == MessageType.AUTHORIZATION_CODE_GRANT_RESPONSE.ordinal()) {
                final Uri uri = (Uri) message.obj;

                Message update;

                try {
                    AuthorizationCodeGrant grant = authorizationServerProxy.
                            parseAuthorizationCodeGrantResponseUrl(getAuthorizationRequest(), uri);

                    if (!getAuthorizationRequest().getState().equals(grant.getState())) {
                        throw new InvalidGrantException("invalid_grant", "mismatching state parameters", null);
                    }

                    AccessToken at = authorizationServerProxy.getAccessToken(grant);
                    setAccessToken(at);

                    update = Message.obtain();
                    update.arg1 = Type.ACCESS_TOKEN.ordinal();
                    update.obj = at;

                    sendResponse(mainMessenger, update);

                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.ACCESS_TOKEN_ACQUIRED.ordinal();

                    replyTo.send(temp);

                } catch (final Exception e) {
                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.ACCESS_TOKEN_NOT_ACQUIRED.ordinal();
                    temp.obj = e;

                    sendResponse(replyTo, temp);
                }

                update = Message.obtain();
                update.arg1 = Type.AUTHORIZATION_REQUEST.ordinal();
                update.obj = null;

                setAuthorizationRequest(null);

                sendResponse(mainMessenger, update);

            } else if (message.arg1 == MessageType.GET_TIME_REQUEST.ordinal()) {
                Message temp = Message.obtain();
                temp.arg1 = MessageType.GET_TIME_RESPONSE.ordinal();

                try {
                    temp.obj = OAuth2WorkerImpl.this.getTime();
                } catch (Exception e) {
                    temp.obj = e;
                }

                sendResponse(replyTo, temp);
            } else if (message.arg1 == MessageType.GET_ATTRIBUTES_REQUEST.ordinal()) {
                Message temp = Message.obtain();
                temp.arg1 = MessageType.GET_ATTRIBUTES_RESPONSE.ordinal();

                try {
                    temp.obj = OAuth2WorkerImpl.this.getAttributes();
                } catch (Exception e) {
                    temp.obj = e;
                }

                if (temp.obj instanceof InvalidTokenException && getAccessToken() != null) {
                    scheduleRefreshAndRedo(message);
                } else {
                    sendResponse(replyTo, temp);
                }
            } else if (message.arg1 == MessageType.GET_MESSAGE_REQUEST.ordinal()) {
                Message temp = Message.obtain();
                temp.arg1 = MessageType.GET_MESSAGE_RESPONSE.ordinal();

                try {
                    temp.obj = OAuth2WorkerImpl.this.getMessage();
                } catch (Exception e) {
                    temp.obj = e;
                }

                if (temp.obj instanceof InvalidTokenException && getAccessToken() != null) {
                    scheduleRefreshAndRedo(message);
                } else {
                    sendResponse(replyTo, temp);
                }
            } else if (message.arg1 == MessageType.SET_MESSAGE_REQUEST.ordinal()) {
                final String text = (String) message.obj;

                Message temp = Message.obtain();
                temp.arg1 = MessageType.SET_MESSAGE_RESPONSE.ordinal();

                try {
                    OAuth2WorkerImpl.this.setMessage(text);
                    temp.obj = OAuth2WorkerImpl.this.getMessage();
                } catch (Exception e) {
                    temp.obj = e;
                }

                if (temp.obj instanceof InvalidTokenException && getAccessToken() != null) {
                    scheduleRefreshAndRedo(message);
                } else {
                    sendResponse(replyTo, temp);
                }
            } else if (message.arg1 == MessageType.OPEN_WEB_SOCKET_REQUEST.ordinal()) {

                Message temp = Message.obtain();
                temp.arg1 = MessageType.OPEN_WEB_SOCKET_RESPONSE.ordinal();

                try {
                    webSocket = new WebSocket(sslContext.getSocketFactory().createSocket());

                    webSocket.connect(echoWebSocketUri, accessToken);

                } catch (Exception e) {
                    if (webSocket != null) {
                        try {
                            webSocket.close();
                        } catch (Exception dummy) {
                            //ignore...
                        }
                    }

                    temp.obj = e;
                }

                sendResponse(replyTo, temp);

            } else if (message.arg1 == MessageType.CLOSE_WEB_SOCKET_REQUEST.ordinal()) {
                try {
                    if (webSocket != null) {
                        webSocket.sendCloseFrame();

                        Frame frame = webSocket.read();

                        Message temp = Message.obtain();
                        temp.arg1 = MessageType.CLOSE_WEB_SOCKET_RESPONSE.ordinal();

                        StringBuilder builder = new StringBuilder();

                        if (frame.getData().length > 0) {
                            int closeCode = frame.getData()[0];
                            closeCode <<= 8;
                            closeCode |= ((int) (frame.getData()[1]) & 255);

                            builder.append("Code: ");
                            builder.append(closeCode);
                        }

                        if (frame.getData().length > 2) {
                            builder.append(" Reason:");
                            builder.append(new String(frame.getData(), 2, frame.getData().length - 2, "UTF-8"));
                        }

                        temp.obj = builder.toString();

                        sendResponse(replyTo, temp);
                    } else {
                        Message temp = Message.obtain();
                        temp.arg1 = MessageType.CLOSE_WEB_SOCKET_RESPONSE.ordinal();
                        temp.obj = "No open websocket";

                        sendResponse(replyTo, temp);
                    }
                } catch (Exception e) {
                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.CLOSE_WEB_SOCKET_RESPONSE.ordinal();
                    temp.obj = e;

                    sendResponse(replyTo, temp);
                }

                if (webSocket != null) {
                    try {
                        webSocket.close();
                    } catch (Exception dummy) {
                        //nothing to do here
                    }

                    webSocket = null;
                }
            } else if (message.arg1 == MessageType.ECHO_REQUEST.ordinal()) {
                final String text = (String) message.obj;
                try {
                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.ECHO_RESPONSE.ordinal();

                    if (webSocket != null) {
                        webSocket.write(new Frame(Operation.TEXT, true, text.getBytes("UTF-8")));

                        Frame frame = webSocket.read();

                        temp.obj = new String(frame.getData(), "UTF-8");
                    } else {
                        temp.obj = "No open websocket";
                    }
                    sendResponse(replyTo, temp);


                } catch (Exception e) {
                    Message temp = Message.obtain();
                    temp.arg1 = MessageType.ECHO_RESPONSE.ordinal();
                    temp.obj = e;

                    sendResponse(replyTo, temp);
                }
            }
        }
    }

    private void sendResponse(Messenger replyTo, Message temp) {
        try {
            replyTo.send(temp);
        } catch (Exception f) {
            //ignore this problem
        }
    }

    private void scheduleRefreshAndRedo(Message message) {
        Message redo = Message.obtain();
        redo.copyFrom(message);
        worker.sendMessageAtFrontOfQueue(redo);

        Message refresh = Message.obtain();
        refresh.arg1 = MessageType.REFRESH_TOKEN.ordinal();

        worker.sendMessageAtFrontOfQueue(refresh);
    }

    public OAuth2WorkerImpl(Context applicationContext) {
        this.applicationContext = applicationContext;

        this.configuration = Configuration.getInstance(applicationContext);
        this.configuration.addObserver(this);

        HandlerThread thread = new HandlerThread("OAuth2WorkerThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);

        thread.start();

        worker = new OAuth2WorkerHandler(thread.getLooper());
        workerMessenger = new Messenger(worker);

        mainMessenger = new Messenger(new MainHandler(applicationContext.getMainLooper()));

        try {
            //TODO: move this into configuration
            echoWebSocketUri = Uri.parse("wss://example.com:8443/oauth-rest/websocket/echo");

            sslContext = getSSLContext();

            resourceServerProxy = new ResourceServerProxy(sslContext.getSocketFactory());

            update(configuration, null);

        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.toString());
        }
    }

    public synchronized AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public synchronized void setAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
    }

    public synchronized AccessToken getAccessToken() {
        return accessToken;
    }

    public synchronized void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public Messenger getWorkerMessenger() {
        return workerMessenger;
    }

    @Override
    public void release() {
        worker.getLooper().quitSafely();
        configuration.deleteObserver(this);
    }

    @Override
    public ConnectionType getNetWorkStatus() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info != null && info.isConnected()) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_BLUETOOTH:
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                    return ConnectionType.FAST;

                default:
                    return ConnectionType.SLOW;
            }

        } else {
            return ConnectionType.NONE;
        }
    }

    @Override
    public synchronized void update(Observable observable, Object object) {
        if (observable == configuration) {
            authorizationServerProxy = new AuthorizationServerProxy(sslContext.getSocketFactory(),
                    CLIENT_ID, CLIENT_PASSWORD, configuration.getAuthorizationEndPointUri(),
                    configuration.getTokenEndPointUri());

            authorizationRequest = configuration.getAuthorizationRequest();
            accessToken = configuration.getAccessToken();
            timeServiceEndPointUri = configuration.getTimeServiceEndPointUri();
            resourceOwnerServiceEndPointUri = configuration.getResourceOwnerEndPointUri();
            messageServiceEndPointUri = configuration.getMessageServiceEndPointUri();
        }
    }

    private AuthorizationRequest buildAuthorizationRequest() {
        Random random = new Random(System.currentTimeMillis());

        String uid = applicationContext.getPackageName() + System.nanoTime() + random.nextLong();

        return new AuthorizationRequest("code", null, Arrays.asList(SCOPES), uid);
    }

    private Uri buildAuthorizationCodeGrantRequestUri() {
        return authorizationServerProxy.getAuthorizationCodeGrantRequestUrl(getAuthorizationRequest());
    }

    private String getTime() throws OAuth2Exception, IOException, URISyntaxException, JSONException {
        JSONObject response = resourceServerProxy.getJSonResource(null, timeServiceEndPointUri);

        return response.toString();
    }

    private String getAttributes() throws OAuth2Exception, IOException, URISyntaxException, JSONException {
        JSONObject response = resourceServerProxy.getJSonResource(getAccessToken(), resourceOwnerServiceEndPointUri);

        return response.toString();
    }

    private String getMessage() throws OAuth2Exception, IOException, URISyntaxException, JSONException {
        JSONObject response =
                resourceServerProxy.getJSonResource(getAccessToken(), messageServiceEndPointUri);

        return response.toString();
    }

    private void setMessage(final String message) throws OAuth2Exception, IOException, URISyntaxException, JSONException {

        JSONObject temp = new JSONObject();
        temp.put("message", message);

        resourceServerProxy.postJSonResource(getAccessToken(), messageServiceEndPointUri, temp);
    }

    private SSLContext getSSLContext() throws IOException, CertificateException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        //read server_crt cert in binary DER format
        InputStream trustedCertStream = applicationContext.getResources().openRawResource(R.raw.server);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        Certificate trustedCertificate =
                certificateFactory.generateCertificate(new BufferedInputStream(trustedCertStream));

        trustedCertStream.close();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        trustStore.load(null);

        trustStore.setCertificateEntry("public", trustedCertificate);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
}
