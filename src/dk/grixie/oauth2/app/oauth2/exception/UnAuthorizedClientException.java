package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

public class UnAuthorizedClientException extends OAuth2Exception {
    public UnAuthorizedClientException(String message, String description, Uri url) {
        super(message, description, url);
    }
}
