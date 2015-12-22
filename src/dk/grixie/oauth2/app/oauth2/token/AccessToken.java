package dk.grixie.oauth2.app.oauth2.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AccessToken {
    private final String accessTokenId;
    private final String type;
    private final long expiresAt;
    private final String refreshTokenId;
    private final String redirectUri;
    private final Collection<String> scope;

    public AccessToken(final String accessTokenId, final String type, final long expiresAt,
                       final String refreshTokenId, final String redirectUri,
                       final Collection<String> scope) {
        this.accessTokenId = accessTokenId;
        this.type = type;
        this.expiresAt = expiresAt;
        this.refreshTokenId = refreshTokenId;
        this.redirectUri = redirectUri;
        this.scope = new ArrayList<String>(scope);
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }

    public String getType() {
        return type;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public Collection<String> getScope() {
        return Collections.unmodifiableCollection(scope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessToken)) return false;

        AccessToken that = (AccessToken) o;

        if (expiresAt != that.expiresAt) return false;
        if (!accessTokenId.equals(that.accessTokenId)) return false;
        if (redirectUri != null ? !redirectUri.equals(that.redirectUri) : that.redirectUri != null) return false;
        if (refreshTokenId != null ? !refreshTokenId.equals(that.refreshTokenId) : that.refreshTokenId != null)
            return false;
        if (!scope.equals(that.scope)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = accessTokenId.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (int) (expiresAt ^ (expiresAt >>> 32));
        result = 31 * result + (refreshTokenId != null ? refreshTokenId.hashCode() : 0);
        result = 31 * result + (redirectUri != null ? redirectUri.hashCode() : 0);
        result = 31 * result + scope.hashCode();
        return result;
    }
}
