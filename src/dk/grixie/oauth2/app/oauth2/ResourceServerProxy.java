package dk.grixie.oauth2.app.oauth2;

import android.net.Uri;
import android.util.Base64;
import dk.grixie.oauth2.app.oauth2.exception.InsufficientScopeException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidRequestException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidTokenException;
import dk.grixie.oauth2.app.oauth2.exception.OAuth2Exception;
import dk.grixie.oauth2.app.oauth2.token.AccessToken;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;

public class ResourceServerProxy {
    private SSLSocketFactory sslSocketFactory;

    public ResourceServerProxy(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public JSONObject getJSonResource(AccessToken accessToken, Uri resourceUrl)
            throws IOException, JSONException, OAuth2Exception {

        URL tokenUrl = new URL(resourceUrl.toString());

        HttpsURLConnection connection = null;

        try {
            connection = (HttpsURLConnection) tokenUrl.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setDoInput(true);
            connection.setDoOutput(false);

            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " +
                        new String(Base64.encode(accessToken.getAccessTokenId().getBytes("UTF-8"),
                                Base64.DEFAULT), "UTF-8"));
            }

            connection.setRequestMethod("GET");

            connection.connect();

            return parseResponse(connection);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect(); //hint to implementation to close socket as well
                }
            } catch (Exception e) {
                //nothing to do here
            }
        }
    }

    public JSONObject postJSonResource(AccessToken accessToken, Uri resourceUrl, JSONObject request)
            throws IOException, JSONException, OAuth2Exception {

        URL tokenUrl = new URL(resourceUrl.toString());

        HttpsURLConnection connection = null;

        try {
            connection = (HttpsURLConnection) tokenUrl.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " +
                        new String(Base64.encode(accessToken.getAccessTokenId().getBytes("UTF-8"),
                                Base64.DEFAULT), "UTF-8"));
            }

            connection.setRequestMethod("POST");

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");

            out.write(request.toString());
            out.close();

            connection.connect();

            return parseResponse(connection);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect(); //hint to implementation to close socket as well
                }
            } catch (Exception e) {
                //nothing to do here
            }
        }
    }

    private JSONObject parseResponse(HttpsURLConnection connection) throws IOException, JSONException, InsufficientScopeException, InvalidTokenException, InvalidRequestException {

        int code;

        try {
            code = connection.getResponseCode();
        } catch (Exception e) {
            code = connection.getResponseCode();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream is = null;

        try {
            is = connection.getInputStream();

            int c;

            while ((c = is.read()) != -1) {
                baos.write(c);
            }
        } catch (Exception e) {
            //
        } finally {
            if (is != null) {
                is.close();
            }
        }

        if (code == 200) {
            return new JSONObject(baos.toString("utf8"));
        } else if (code == 204) {
            return null;
        } else {
            String error = null;
            String description = null;
            Uri url = null;
            String scope = "";

            String authHeader = connection.getHeaderField("WWW-Authenticate");

            if (authHeader != null) {
                String t = extract("error_uri", authHeader);

                if (t != null) {
                    url = Uri.parse(t);
                }

                description = extract("error_description", authHeader);
                error = extract("error", authHeader);
                scope = extract("scope", authHeader);
            }

            if (connection.getResponseCode() == 403) {
                throw new InsufficientScopeException("insufficient_scope",
                        description != null ? description : "Insufficient scope to perform operation",
                        url, Arrays.asList(scope.split(" ")));
            } else if (connection.getResponseCode() == 401) {
                if (error != null) {
                    throw new InvalidTokenException("invalid_token",
                            description != null ? description :
                                    "The token has been revoked, expired or is invalid", url);
                } else {
                    throw new InvalidTokenException("invalid_token", "No token provided", null);
                }
            } else {
                throw new InvalidRequestException("invalid_request",
                        description != null ? description : "description", url);
            }
        }
    }

    private String extract(String key, String header) {
        int start = header.indexOf(key);

        if (start >= 0) {
            start = header.indexOf('"', start);

            int end = header.indexOf('"', start + 1);

            return header.substring(start + 1, end);
        }

        return null;
    }
}
