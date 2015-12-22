package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class InvalidGrantException extends OAuth2Exception {
    public InvalidGrantException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
