package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class InvalidClientException extends OAuth2Exception {
    public InvalidClientException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
