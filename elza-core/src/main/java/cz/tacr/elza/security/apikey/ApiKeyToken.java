package cz.tacr.elza.security.apikey;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parsed form of an {@code X-API-Key} token: {@code elza_<keyId>_<secret>}.
 *
 * The pattern accepts nothing but the exact shape, so an accidental Bearer token, a header value
 * with whitespace, or a garbled copy is rejected with {@link Optional#empty()} rather than being
 * silently trimmed. The caller turns that into a MALFORMED_TOKEN response.
 */
public record ApiKeyToken(String keyId, String secret) {

    private static final String PREFIX = "elza_";
    /** {@code elza_<keyId>_<secret>} — both parts are base62. */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("elza_([A-Za-z0-9]+)_([A-Za-z0-9]+)");

    public static Optional<ApiKeyToken> parse(String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            return Optional.empty();
        }
        var matcher = TOKEN_PATTERN.matcher(header);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ApiKeyToken(matcher.group(1), matcher.group(2)));
    }
}
