/**
 * Kopírování do schránky.
 *
 * `navigator.clipboard` je dostupné pouze v zabezpečeném kontextu (https, resp. localhost).
 * ELZA běží na řadě instalací po http, kde je celé `navigator.clipboard` undefined — proto
 * je zde záložní cesta přes skrytý textarea a `document.execCommand('copy')`.
 */
export async function copyTextToClipboard(text: string): Promise<boolean> {
    if (text == null) {
        return false;
    }

    if (navigator.clipboard) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch {
            // Zápis může selhat i v zabezpečeném kontextu (odepřené oprávnění,
            // dokument bez fokusu) — zkusíme ještě záložní cestu.
        }
    }

    return copyUsingTextArea(text);
}

function copyUsingTextArea(text: string): boolean {
    const el = document.createElement('textarea');
    el.value = text;
    // Mimo viewport, ať kopírování nezpůsobí odskočení stránky.
    el.style.position = 'fixed';
    el.style.top = '-1000px';
    el.setAttribute('readonly', '');

    document.body.appendChild(el);
    try {
        el.select();
        return document.execCommand('copy');
    } catch {
        return false;
    } finally {
        document.body.removeChild(el);
    }
}
