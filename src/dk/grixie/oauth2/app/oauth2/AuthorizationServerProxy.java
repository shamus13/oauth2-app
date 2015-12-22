package dk.grixie.oauth2.app.oauth2;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import dk.grixie.oauth2.app.oauth2.exception.AccessDeniedException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidClientException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidGrantException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidRequestException;
import dk.grixie.oauth2.app.oauth2.exception.InvalidScopeException;
import dk.grixie.oauth2.app.oauth2.exception.OAuth2Exception;
import dk.grixie.oauth2.app.oauth2.exception.UnAuthorizedClientException;
import dk.grixie.oauth2.app.oauth2.exception.UnAuthorizedException;
import dk.grixie.oauth2.app.oauth2.exception.UnsupportedGrantTypeException;
import dk.grixie.oauth2.app.oauth2.token.AccessToken;
import dk.grixie.oauth2.app.oauth2.token.AuthorizationCodeGrant;
import dk.grixie.oauth2.app.oauth2.token.AuthorizationRequest;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthorizationServerProxy {

    private SSLSocketFactory sslSocketFactory;

    private String id;
    private String password;
    private Uri authorizationEndpoint;
    private Uri tokenEndpoint;

    public AuthorizationServerProxy(SSLSocketFactory sslSocketFactory,
                                    String id, String password, Uri authorizationEndpoint, Uri tokenEndpoint) {
        this.sslSocketFactory = sslSocketFactory;
        this.id = id;
        this.password = password;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
    }

    public Uri getAuthorizationCodeGrantRequestUrl(AuthorizationRequest request) {
        Uri.Builder builder = authorizationEndpoint.buildUpon();

        builder.appendQueryParameter("client_id", id).
                appendQueryParameter("response_type", request.getType());

        if (request.getRedirectUri() != null) {
            builder.appendQueryParameter("redirect_uri", request.getRedirectUri());
        }

        if (request.getScope().size() > 0) {
            builder.appendQueryParameter("scope", TextUtils.join(" ", request.getScope()));
        }

        if (request.getState() != null) {
            builder.appendQueryParameter("state", request.getState());
        }

        return builder.build();
    }

    public AuthorizationCodeGrant parseAuthorizationCodeGrantResponseUrl(AuthorizationRequest request,
                                                                         Uri responseUrl)
            throws OAuth2Exception, URISyntaxException, MalformedURLException {
        String state = responseUrl.getQueryParameter("state");
        String code = responseUrl.getQueryParameter("code");
        String error = responseUrl.getQueryParameter("error");
        String errorDescription = responseUrl.getQueryParameter("error_description");
        Uri errorUri = responseUrl.getQueryParameter("error_uri") != null ? Uri.parse(responseUrl.getQueryParameter("error_uri")) : null;

        if (code != null) {
            return new AuthorizationCodeGrant(code, request.getRedirectUri(), request.getScope(), state);
        } else if (error != null) {
            if (error.equals("invalid_request")) {
                throw new InvalidRequestException(error, errorDescription, errorUri);
            } else if (error.equals("invalid_client")) {
                throw new InvalidClientException(error, errorDescription, errorUri);
            } else if (error.equals("invalid_grant")) {
                throw new InvalidGrantException(error, errorDescription, errorUri);
            } else if (error.equals("unauthorized_client")) {
                throw new UnAuthorizedClientException(error, errorDescription, errorUri);
            } else if (error.equals("unsupported_grant_type")) {
                throw new UnsupportedGrantTypeException(error, errorDescription, errorUri);
            } else if (error.equals("invalid_scope")) {
                throw new InvalidScopeException(error, errorDescription, errorUri);
            } else if (error.equals("access_denied")) {
                throw new AccessDeniedException(error, errorDescription, errorUri);
            } else {
                throw new OAuth2Exception("unknown server error",
                        "the server returned an unrecognized error", null);
            }

        }

        throw new OAuth2Exception("invalid response",
                "server did not return a valid OAuth 2.0 response", null);
    }

    public AccessToken getAccessToken(AuthorizationCodeGrant grant)
            throws OAuth2Exception, URISyntaxException, IOException {
        if (grant != null) {
            Map<String, Collection<String>> parameters = new HashMap<String, Collection<String>>();

            parameters.put("client_id", Arrays.asList(id));
            parameters.put("grant_type", Arrays.asList("authorization_code"));
            parameters.put("code", Arrays.asList(grant.getCode()));

            if (grant.getRedirectUri() != null) {
                parameters.put("redirect_uri", Arrays.asList(grant.getRedirectUri()));
            }

            Pair<Integer, String> result = postForm(parameters);

            return parseAccessTokenResponse(grant.getRedirectUri(), grant.getScope(), result.second, result.first);
        } else {
            throw new InvalidRequestException("invalid_request", "no authorization code grant provided", null);
        }
    }

    public AccessToken refreshAccessToken(AccessToken token)
            throws OAuth2Exception, URISyntaxException, IOException {
        if (token != null) {
            Map<String, Collection<String>> parameters = new HashMap<String, Collection<String>>();

            parameters.put("client_id", Arrays.asList(id));
            parameters.put("grant_type", Arrays.asList("refresh_token"));
            parameters.put("refresh_token", Arrays.asList(token.getRefreshTokenId()));

            if (token.getRedirectUri() != null) {
                parameters.put("redirect_uri", Arrays.asList(token.getRedirectUri()));
            }

            Pair<Integer, String> result = postForm(parameters);

            return parseAccessTokenResponse(token.getRedirectUri(), token.getScope(), result.second, result.first);
        } else {
            throw new InvalidRequestException("invalid_request", "no access token provided", null);
        }
    }

    private AccessToken parseAccessTokenResponse(final String redirectUri,
                                                 final Collection<String> defaultScope,
                                                 final String result,
                                                 final int statusCode) throws OAuth2Exception, MalformedURLException {
        try {
            JSONObject response = new JSONObject(result);

            if (response.has("access_token")) {
                String id = response.getString("access_token");
                String type = response.getString("token_type");
                long expiresAt = 0;
                String refreshToken = null;
                Collection<String> scope = defaultScope;

                if (response.has("expires_in")) {
                    expiresAt = new Date().getTime() + response.getLong("expires_in");
                }

                if (response.has("refresh_token")) {
                    refreshToken = response.getString("refresh_token");
                }

                if (response.has("scope")) {
                    scope = Arrays.asList(response.getString("scope").split(" "));
                }

                return new AccessToken(id, type, expiresAt, refreshToken, redirectUri, scope);
            } else if (response.has("error")) {
                String code = response.getString("error");
                String description = response.has("error_description") ?
                        response.getString("error_description") : null;
                Uri url = response.has("error_uri") ?
                        Uri.parse(response.getString("error_uri")) : null;

                if (response.getString("error").equals("invalid_request")) {
                    throw new InvalidRequestException(code, description, url);
                } else if (response.getString("error").equals("invalid_client")) {
                    if (statusCode == 401) {
                        throw new UnAuthorizedException(code, description, url);
                    } else {
                        throw new InvalidClientException(code, description, url);
                    }
                } else if (response.getString("error").equals("invalid_grant")) {
                    throw new InvalidGrantException(code, description, url);
                } else if (response.getString("error").equals("unauthorized_client")) {
                    throw new UnAuthorizedClientException(code, description, url);
                } else if (response.getString("error").equals("unsupported_grant_type")) {
                    throw new UnsupportedGrantTypeException(code, description, url);
                } else if (response.getString("error").equals("invalid_scope")) {
                    throw new InvalidScopeException(code, description, url);
                } else {
                    throw new OAuth2Exception("unknown server error",
                            "the server returned an unrecognized error", null);
                }
            }
        } catch (JSONException e) {
            Log.w("oauth2-app", "response is not valid json, this is probably not a token end point", e);
            throw new OAuth2Exception("invalid response",
                    "server did not return a valid OAuth 2.0 response", null);
        }
        throw new OAuth2Exception("invalid response",
                "server did not return a valid OAuth 2.0 response", null);
    }

    private Pair<Integer, String> postForm(Map<String, Collection<String>> parameters) throws IOException {

        URL tokenUrl = new URL(tokenEndpoint.toString());

        HttpsURLConnection connection = null;

        try {
            connection = (HttpsURLConnection) tokenUrl.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");

            if (password != null) {
                connection.setRequestProperty("Authorization", "Basic " +
                        new String(Base64.encode((id + ":" + password).getBytes("UTF-8"), Base64.DEFAULT), "UTF-8"));
            }

            String message = encodeFormPostParameters(parameters);

            connection.setRequestProperty("Content-Length", "" + Integer.toString(message.getBytes().length));
            connection.setRequestMethod("POST");

            connection.connect();

            BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(),
                    "UTF-8"));

            bos.write(message);

            bos.close();

            int code;

            try {
                code = connection.getResponseCode();
            } catch (Exception e) {
                code = connection.getResponseCode();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream is = null;

            try {
                if (code == HttpsURLConnection.HTTP_OK) {
                    is = connection.getInputStream();
                } else {
                    is = connection.getErrorStream();
                }

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

            return new Pair<Integer, String>(code, baos.toString());
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

    private String encodeFormPostParameters(Map<String, Collection<String>> parameters) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();

        for (String key : parameters.keySet()) {
            for (String value : parameters.get(key)) {
                if (builder.length() != 0) {
                    builder.append('&');
                }

                builder.append(key);
                builder.append('=');
                builder.append(URLEncoder.encode(value, "utf8"));
            }
        }

        return builder.toString();
    }

}
