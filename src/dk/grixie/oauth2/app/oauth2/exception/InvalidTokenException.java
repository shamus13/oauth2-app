package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class InvalidTokenException extends OAuth2Exception {
    public InvalidTokenException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
