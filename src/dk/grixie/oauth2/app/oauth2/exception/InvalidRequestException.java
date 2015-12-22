package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class InvalidRequestException extends OAuth2Exception {
    public InvalidRequestException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
