package dk.grixie.oauth2.app.oauth2.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AuthorizationCodeGrant {
    private final String code;
    private final String redirectUri;
    private final Collection<String> scope;
    private final String state;

    public AuthorizationCodeGrant(final String code, final String redirectUri,
                                  final Collection<String> scope, final String state) {
        this.code = code;
        this.redirectUri = redirectUri;
        this.scope = new ArrayList<String>(scope);
        this.state = state;
    }

    public String getCode() {
        return code;
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
