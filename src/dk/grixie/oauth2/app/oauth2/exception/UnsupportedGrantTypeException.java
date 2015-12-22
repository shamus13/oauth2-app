package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class UnsupportedGrantTypeException extends OAuth2Exception {
    public UnsupportedGrantTypeException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
