package cz.tacr.elza.security.apikey;

/**
 * Reason an API-key authentication attempt was refused. Kept as an enum so the entry point can
 * write a stable {@code code} the client can react to, distinct from the human-readable message.
 */
public enum ApiKeyFailure {
    MALFORMED_TOKEN("Token nemá platný tvar; použijte hodnotu, kterou vydal server."),
    UNKNOWN_KEY("Klíč s tímto identifikátorem neexistuje."),
    INVALID_SECRET("Tajná část tokenu neodpovídá."),
    EXPIRED("Platnost klíče vypršela. Vytvořte nový."),
    REVOKED("Klíč byl zrušen. Vytvořte nový."),
    USER_INACTIVE("Uživatel je neaktivní. Kontaktujte správce.");

    private final String message;

    ApiKeyFailure(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
