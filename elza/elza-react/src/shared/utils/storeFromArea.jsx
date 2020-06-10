import {indexById} from 'stores/app/utils.jsx';

/**
 * Načtení store pro konkrétní areu.
 * @param store root store aplikace, pod kterým je očekáván store app!
 * @param area area
 * @param showErrors mají se zobrazovat chyby v případě něnalezení store?
 * @return {*}
 */
export default function storeFromArea(store, area, showErrors = true) {
    let st;

    if (area.indexOf('shared.') === 0) {
        let useArea = area.substring('shared.'.length);
        if (!store.app.shared) {
            if (showErrors) {
                console.error(`Store shared is not defined in`, store.app);
            }
            return null;
        } else {
            const useStore = store.app.shared.stores[useArea];
            if (!useStore) {
                if (showErrors) {
                    console.error(`Store shared.stores[${useArea}] is not defined in`, store.app.shared);
                }
                return null;
            } else {
                st = useStore;
            }
        }
    } else if (area.indexOf('refTables.') === 0) {
        let useArea = area.substring('refTables.'.length);
        let useStore = store.refTables[useArea];
        if (!useStore) {
            if (showErrors) {
                console.error(`Store refTables[${useArea}] is not defined in`, store.refTables);
            }
            return null;
        } else {
            st = useStore;
        }
    } else {
        const areaItems = area.split('.');

        if (areaItems[0].startsWith('fund[')) {
            st = store.arrRegion;
        } else if (areaItems[0] === 'adminRegion') {
            st = store;
        } else {
            st = store.app;
        }

        for (let a = 0, len = areaItems.length; a < len; a++) {
            const item = areaItems[a];

            // Variabilita zpracování area i do fund store na základě předaných dat
            if (item.startsWith('fund[')) {
                const versionId = item.substring('fund['.length, item.indexOf(']'));
                const index = indexById(st.funds, parseInt(versionId), 'versionId');
                const subStore = st.funds[index];
                if (subStore) {
                    st = subStore;
                } else {
                    const path = areaItems.slice(a).join('.');
                    if (showErrors) {
                        console.error(
                            `Store ${path} is not defined in`,
                            st,
                            `Cannot resolve full path 'app.${area}' in root store`,
                            store,
                        );
                    }
                    return null;
                }
            } else {
                const subStore = st[item];
                if (subStore) {
                    st = subStore;
                } else {
                    const path = areaItems.slice(a).join('.');
                    if (showErrors) {
                        console.error(
                            `Store ${path} is not defined in`,
                            st,
                            `Cannot resolve full path 'app.${area}' in root store`,
                            store,
                        );
                    }
                    return null;
                }
            }
        }
    }

    return st;
}
