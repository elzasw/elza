package cz.tacr.elza.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import cz.tacr.elza.security.oauth2.OAuth2Properties;

/**
 * Issuer and audience of a token are checked only when configured.
 */
class JwtValidatorTest {

    private static Jwt token(String issuer, List<String> audiences) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("jdoe")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .issuer(issuer);
        if (audiences != null) {
            builder.audience(audiences);
        }
        return builder.build();
    }

    private static boolean isValid(OAuth2Properties props, Jwt jwt) {
        OAuth2TokenValidator<Jwt> validator = ApplicationSecurity.createJwtValidator(props);
        return !validator.validate(jwt).hasErrors();
    }

    @Test
    void anyIssuerAndAudienceAcceptedWhenNotConfigured() {
        OAuth2Properties props = new OAuth2Properties();

        assertTrue(isValid(props, token("https://other.example", null)));
    }

    @Test
    void issuerIsChecked() {
        OAuth2Properties props = new OAuth2Properties();
        props.setIssuer("https://idp.example");

        assertTrue(isValid(props, token("https://idp.example", null)));
        assertFalse(isValid(props, token("https://other.example", null)));
    }

    @Test
    void oneOfAudiencesIsRequired() {
        OAuth2Properties props = new OAuth2Properties();
        props.setAudiences(List.of("elza", "elza-api"));

        assertTrue(isValid(props, token("https://idp.example", List.of("account", "elza-api"))));
        assertFalse(isValid(props, token("https://idp.example", List.of("account"))));
        assertFalse(isValid(props, token("https://idp.example", null)));
    }

    @Test
    void expiredTokenIsRejected() {
        Jwt expired = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("jdoe")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(1800))
                .build();

        assertFalse(isValid(new OAuth2Properties(), expired));
    }
}
