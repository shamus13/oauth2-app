package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class OAuth2Exception extends Exception {
    private final String description;
    private final Uri uri;

    public OAuth2Exception(String message, String description, Uri uri) {
        super(message);
        this.description = description;
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public Uri getUri() {
        return uri;
    }
}
