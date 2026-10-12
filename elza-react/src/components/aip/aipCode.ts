const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const UUID_PREFIX = 'uuid-';

/**
 * The forms an identifier of an AIP or of its part may be stored in: as written first, then -
 * for a UUID - bare and with the "uuid-" prefix, in lower case. A package writes a UUID with
 * the prefix (an XML ID must not start with a digit), the digital archive without it.
 */
export function aipCodeForms(value: string): string[] {
    const code = value.trim();
    const bare = code.toLowerCase().startsWith(UUID_PREFIX) ? code.substring(UUID_PREFIX.length) : code;
    if (!UUID.test(bare)) {
        return [code];
    }
    return [...new Set([code, bare.toLowerCase(), UUID_PREFIX + bare.toLowerCase()])];
}

/** Whether two identifiers are the same, whichever form each is written in. */
export function sameAipCode(a: string, b: string): boolean {
    const forms = aipCodeForms(b);
    return aipCodeForms(a).some(form => forms.includes(form));
}
