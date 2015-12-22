package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class AccessDeniedException extends OAuth2Exception {
    public AccessDeniedException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
