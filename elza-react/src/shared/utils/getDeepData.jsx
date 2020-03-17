/**
 * Načtení dat pro konkrétní otečkovanou cestu z objektu.
 * @param obj objekt
 * @param path cesta k datům, např. a.b.c
 */
export default function getDeepData(obj, path) {
    const attrs = path.split('.');
    var o = obj;
    for (let a = 0; a < attrs.length; a++) {
        if (a + 1 < attrs.length) {
            // není poslední
            if (!o[a]) {
                return null; // vnořený objekt není definován
            }
            o = o[a];
        } else {
            // je poslední - konkrétní hodnota
            return o[a];
        }
        return null;
    }
}
