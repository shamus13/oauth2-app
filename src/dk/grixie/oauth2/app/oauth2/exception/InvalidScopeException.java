package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class InvalidScopeException extends OAuth2Exception {
    public InvalidScopeException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
