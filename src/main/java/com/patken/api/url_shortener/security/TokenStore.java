package com.patken.api.url_shortener.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Hazelcast-backed revocation state. This is the deliberate bit of server-side state that makes
 * JWTs revocable — kept off the hot path (only touched at login / refresh / logout), so stateless
 * validation of access tokens stays cheap.
 * <ul>
 *     <li>{@code REFRESH_TOKENS}: username -&gt; currently-active refresh token id. Rotating or
 *     revoking replaces/removes it, which invalidates any previously issued refresh token.</li>
 *     <li>{@code TOKENS_VALID_AFTER}: username -&gt; epoch second before which every access token is
 *     rejected. Bumped on logout to revoke all outstanding access tokens for that user at once.</li>
 * </ul>
 */
@Component
public class TokenStore {

    private final IMap<String, String> activeRefreshTokens;
    private final IMap<String, Long> tokensValidAfter;

    public TokenStore(HazelcastInstance hazelcastInstance) {
        this.activeRefreshTokens = hazelcastInstance.getMap("REFRESH_TOKENS");
        this.tokensValidAfter = hazelcastInstance.getMap("TOKENS_VALID_AFTER");
    }

    public void storeActiveRefresh(String username, String refreshId) {
        activeRefreshTokens.put(username, refreshId);
    }

    public boolean isActiveRefresh(String username, String refreshId) {
        return refreshId != null && refreshId.equals(activeRefreshTokens.get(username));
    }

    /** Revoke everything for the user: refresh token and all outstanding access tokens. */
    public void revokeAll(String username) {
        activeRefreshTokens.remove(username);
        tokensValidAfter.put(username, Instant.now().getEpochSecond());
    }

    public Optional<Long> validAfterEpochSecond(String username) {
        return Optional.ofNullable(tokensValidAfter.get(username));
    }
}
