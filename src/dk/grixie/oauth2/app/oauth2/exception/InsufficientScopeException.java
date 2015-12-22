package dk.grixie.oauth2.app.oauth2.exception;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collection;

public class InsufficientScopeException extends OAuth2Exception {
    private final Collection<String> scopes;

    public InsufficientScopeException(String message, String description, Uri url,
                                      Collection<String> scopes) {
        super(message, description, url);
        this.scopes = new ArrayList<String>(scopes);
    }

    public Collection<String> getScopes() {
        return scopes;
    }
}
