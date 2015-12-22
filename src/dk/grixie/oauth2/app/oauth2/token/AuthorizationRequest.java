package dk.grixie.oauth2.app.oauth2.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AuthorizationRequest {
    private final String type;
    private final String redirectUri;
    private final Collection<String> scope;
    private final String state;

    public AuthorizationRequest(final String type, final String redirectUri,
                                final Collection<String> scope, final String state) {
        this.type = type;
        this.redirectUri = redirectUri;
        this.scope = new ArrayList<String>(scope);
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public Collection<String> getScope() {
        return Collections.unmodifiableCollection(scope);
    }

    public String getState() {
        return state;
    }
}
