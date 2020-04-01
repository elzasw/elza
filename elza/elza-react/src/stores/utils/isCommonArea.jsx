/**
 * Test, zda se jedná o area, která je standardní pro zpracování - tečkový zápis area, vyloučí to area, které jsou použité např. na strom JP apod.
 * @param area
 */
export default function isCommonArea(area) {
    if (
        area &&
        typeof area === 'string' &&
        !area.startsWith('FUND_TREE') &&
        !area.startsWith('NODE') &&
        !area.startsWith('OUTPUT') &&
        !area.startsWith('STRUCTURE')
    ) {
        // zpracovává se jako area, ale stromy nechceme, tam je area z jiného důvodu
        return true;
    } else {
        return false;
    }
}
