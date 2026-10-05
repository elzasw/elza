/**
 * Set when the user logs out on purpose, so the Kerberos sign-in does not sign them
 * straight back in. Deliberately not persisted - a reload is a request to start over.
 */
let autoSsoLoginSuppressed = false;

export const suppressAutoSsoLogin = () => {
    autoSsoLoginSuppressed = true;
};

export const isAutoSsoLoginSuppressed = () => autoSsoLoginSuppressed;
