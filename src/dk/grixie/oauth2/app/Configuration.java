package dk.grixie.oauth2.app;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import dk.grixie.oauth2.app.oauth2.token.AccessToken;
import dk.grixie.oauth2.app.oauth2.token.AuthorizationRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;

public class Configuration extends Observable {
    private static final String FILENAME = "config";

    private static final String AUTHORIZATION_ENDPOINT_URI = "https://example.com/module.php/oauth2server/authorization/authorization.php";
    private static final String TOKEN_ENDPOINT_URI = "https://example.com/module.php/oauth2server/authorization/token.php";
    private static final String RESOURCE_OWNER_ENDPOINT_URI = "https://example.com/module.php/oauth2server/resource/owner.php";
    private static final String TIME_SERVICE_ENDPOINT_URI = "https://example.com:8443/oauth-rest/rest/test/time";
    private static final String MESSAGE_SERVICE_ENDPOINT_URI = "https://example.com:8443/oauth-rest/rest/test/message";

    private Uri authorizationEndPointUri = null;
    private Uri tokenEndPointUri = null;
    private Uri resourceOwnerEndPointUri = null;
    private Uri timeServiceEndPointUri = null;
    private Uri messageServiceEndPointUri = null;

    private AuthorizationRequest authorizationRequest = null;
    private AccessToken accessToken = null;

    private Storage storage = null;

    private static Configuration instance = null;

    public static synchronized Configuration getInstance(Context context) {
        if (instance == null) {
            instance = new Configuration(context);
        }

        return instance;
    }

    private Configuration(Context applicationContext) {
        this.storage = new Storage(applicationContext.getDir("config", Context.MODE_PRIVATE));

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... a) {
                loadState();

                return null;
            }

            @Override
            protected void onPostExecute(Void a) {
                setChanged();

                notifyObservers();
            }
        }.execute();
    }

    public synchronized AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public synchronized void setAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        if ((this.authorizationRequest != null && !this.authorizationRequest.equals(authorizationRequest)) ||
                (this.authorizationRequest == null && authorizationRequest != null)) {

            this.authorizationRequest = authorizationRequest;

            startBackgroundSave();
        }
    }

    public synchronized AccessToken getAccessToken() {
        return accessToken;
    }

    public synchronized void setAccessToken(AccessToken accessToken) {
        if ((this.accessToken != null && !this.accessToken.equals(accessToken)) ||
                (this.accessToken == null && accessToken != null)) {

            this.accessToken = accessToken;

            startBackgroundSave();
        }
    }

    public synchronized Uri getAuthorizationEndPointUri() {
        return authorizationEndPointUri;
    }

    public synchronized void setAuthorizationEndPointUri(Uri authorizationEndPointUri) {
        if ((this.authorizationEndPointUri != null && !this.authorizationEndPointUri.equals(authorizationEndPointUri)) ||
                (this.authorizationEndPointUri == null && authorizationEndPointUri != null)) {

            this.authorizationEndPointUri = authorizationEndPointUri;

            startBackgroundSave();
        }
    }

    public synchronized Uri getTokenEndPointUri() {
        return tokenEndPointUri;
    }

    public synchronized void setTokenEndPointUri(Uri tokenEndPointUri) {
        if ((this.tokenEndPointUri != null && !this.tokenEndPointUri.equals(tokenEndPointUri)) ||
                (this.tokenEndPointUri == null && tokenEndPointUri != null)) {

            this.tokenEndPointUri = tokenEndPointUri;

            startBackgroundSave();
        }
    }

    public synchronized Uri getResourceOwnerEndPointUri() {
        return resourceOwnerEndPointUri;
    }

    public synchronized void setResourceOwnerEndPointUri(Uri resourceOwnerEndPointUri) {
        if ((this.resourceOwnerEndPointUri != null && !this.resourceOwnerEndPointUri.equals(resourceOwnerEndPointUri)) ||
                (this.resourceOwnerEndPointUri == null && resourceOwnerEndPointUri != null)) {

            this.resourceOwnerEndPointUri = resourceOwnerEndPointUri;

            startBackgroundSave();
        }
    }

    public synchronized Uri getTimeServiceEndPointUri() {
        return timeServiceEndPointUri;
    }

    public synchronized void setTimeServiceEndPointUri(Uri timeServiceEndPointUri) {
        if ((this.timeServiceEndPointUri != null && !this.timeServiceEndPointUri.equals(timeServiceEndPointUri)) ||
                (this.timeServiceEndPointUri == null && timeServiceEndPointUri != null)) {

            this.timeServiceEndPointUri = timeServiceEndPointUri;

            startBackgroundSave();
        }
    }

    public synchronized Uri getMessageServiceEndPointUri() {
        return messageServiceEndPointUri;
    }

    public synchronized void setMessageServiceEndPointUri(Uri messageServiceEndPointUri) {
        if ((this.messageServiceEndPointUri != null && !this.messageServiceEndPointUri.equals(messageServiceEndPointUri)) ||
                (this.messageServiceEndPointUri == null && messageServiceEndPointUri != null)) {

            this.messageServiceEndPointUri = messageServiceEndPointUri;

            startBackgroundSave();
        }
    }

    private void startBackgroundSave() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... a) {
                saveState();

                return null;
            }
        }.execute();

        setChanged();
        notifyObservers();
    }

    private synchronized void loadState() {
        try {
            authorizationEndPointUri = Uri.parse(AUTHORIZATION_ENDPOINT_URI);
            tokenEndPointUri = Uri.parse(TOKEN_ENDPOINT_URI);
            resourceOwnerEndPointUri = Uri.parse(RESOURCE_OWNER_ENDPOINT_URI);
            timeServiceEndPointUri = Uri.parse(TIME_SERVICE_ENDPOINT_URI);
            messageServiceEndPointUri = Uri.parse(MESSAGE_SERVICE_ENDPOINT_URI);
        } catch (Exception e) {
            //impossible unless the default configuration is messed up
            Log.e(this.getClass().getName(), "Failed to parse default url's. " + e.getMessage());
        }

        for (String id : storage.getFileNames()) {
            BufferedReader reader = null;

            try {
                if (id.equals(FILENAME)) {
                    reader = new BufferedReader(storage.getInputStreamReader(id));

                    JSONObject oauth2 = new JSONObject(reader.readLine());

                    if (oauth2.has("authorizationEndPointUri")) {
                        authorizationEndPointUri = Uri.parse(oauth2.getString("authorizationEndPointUri"));
                    } else {
                        authorizationEndPointUri = Uri.parse(AUTHORIZATION_ENDPOINT_URI);
                    }

                    if (oauth2.has("tokenEndPointUri")) {
                        tokenEndPointUri = Uri.parse(oauth2.getString("tokenEndPointUri"));
                    } else {
                        tokenEndPointUri = Uri.parse(TOKEN_ENDPOINT_URI);
                    }

                    if (oauth2.has("resourceOwnerEndPointUri")) {
                        resourceOwnerEndPointUri = Uri.parse(oauth2.getString("resourceOwnerEndPointUri"));
                    } else {
                        resourceOwnerEndPointUri = Uri.parse(RESOURCE_OWNER_ENDPOINT_URI);
                    }

                    if (oauth2.has("timeServiceEndPointUri")) {
                        timeServiceEndPointUri = Uri.parse(oauth2.getString("timeServiceEndPointUri"));
                    } else {
                        timeServiceEndPointUri = Uri.parse(TIME_SERVICE_ENDPOINT_URI);
                    }

                    if (oauth2.has("messageServiceEndPointUri")) {
                        messageServiceEndPointUri = Uri.parse(oauth2.getString("messageServiceEndPointUri"));
                    } else {
                        messageServiceEndPointUri = Uri.parse(MESSAGE_SERVICE_ENDPOINT_URI);
                    }

                    if (oauth2.has("accessToken")) {
                        JSONObject at = oauth2.getJSONObject("accessToken");

                        Collection<String> scope;
                        if (at.has("scope")) {
                            scope = deserializeScope(at.getJSONArray("scope"));
                        } else {
                            scope = new ArrayList<String>();
                        }

                        accessToken = new AccessToken(at.getString("accessTokenId"),
                                at.getString("type"), at.getLong("expiresAt"),
                                at.getString("refreshTokenId"),
                                at.optString("redirectUri", null),
                                scope);
                    }

                    if (oauth2.has("authorizationRequest")) {
                        JSONObject ar = oauth2.getJSONObject("authorizationRequest");

                        Collection<String> scope;
                        if (ar.has("scope")) {
                            scope = deserializeScope(ar.getJSONArray("scope"));
                        } else {
                            scope = new ArrayList<String>();
                        }

                        authorizationRequest = new AuthorizationRequest(ar.getString("type"),
                                ar.optString("redirectUri", null), scope,
                                ar.has("state") ? ar.getString("state") : null);
                    }
                }
            } catch (Exception e) {
                Log.e(this.getClass().getName(), e.toString());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //nothing to do here any way
                    }
                }
            }
        }
    }

    private synchronized void saveState() {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(storage.getOutputStreamWriter(FILENAME));

            JSONObject temp = new JSONObject();

            if (authorizationEndPointUri != null) {
                temp.put("authorizationEndPointUri", authorizationEndPointUri);
            }

            if (tokenEndPointUri != null) {
                temp.put("tokenEndPointUri", tokenEndPointUri);
            }

            if (resourceOwnerEndPointUri != null) {
                temp.put("resourceOwnerEndPointUri", resourceOwnerEndPointUri);
            }

            if (timeServiceEndPointUri != null) {
                temp.put("timeServiceEndPointUri", timeServiceEndPointUri);
            }

            if (messageServiceEndPointUri != null) {
                temp.put("messageServiceEndPointUri", messageServiceEndPointUri);
            }

            if (getAccessToken() != null) {
                AccessToken token = getAccessToken();

                JSONObject at = new JSONObject();

                at.put("accessTokenId", token.getAccessTokenId());
                at.put("type", token.getType());
                at.put("expiresAt", token.getExpiresAt());
                at.put("refreshTokenId", token.getRefreshTokenId());

                if (token.getRedirectUri() != null) {
                    at.put("redirectUri", token.getRedirectUri());
                }

                at.put("scope", serializeScope(token.getScope()));

                temp.put("accessToken", at);
            }

            if (getAuthorizationRequest() != null) {
                AuthorizationRequest request = getAuthorizationRequest();

                JSONObject ar = new JSONObject();

                ar.put("type", request.getType());

                if (request.getRedirectUri() != null) {
                    ar.put("redirectUri", request.getRedirectUri());
                }

                ar.put("scope", serializeScope(request.getScope()));

                if (request.getState() != null) {
                    ar.put("state", request.getState());
                }

                temp.put("authorizationRequest", ar);
            }

            writer.write(temp.toString());
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.toString());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //nothing to do here any way
                }
            }
        }
    }

    private static JSONArray serializeScope(Collection<String> scopes) throws JSONException {
        JSONArray result = new JSONArray();

        for (String scope : scopes) {
            result.put(scope);
        }

        return result;
    }

    private static Collection<String> deserializeScope(JSONArray scopes) throws JSONException {
        Collection<String> result = new ArrayList<String>();

        for (int i = 0; i < scopes.length(); ++i) {
            if (!scopes.isNull(i)) {
                result.add(scopes.getString(i));
            }
        }

        return result;
    }
}
