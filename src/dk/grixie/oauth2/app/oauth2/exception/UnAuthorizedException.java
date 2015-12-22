package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class UnAuthorizedException extends OAuth2Exception {
    public UnAuthorizedException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
