import React from 'react';

/**
 * Provede merge addons pro input prvky.
 * @param propsAddons addons předné do komponenty přes props, např. addonsBefore
 * @param customAddons vlastní addons komponenty
 * @param customsAtBegin true, pokud se mají custom přidat před props
 * @return nové addons
 */
export default function mergeAddons(propsAddons, customAddons, customsAtBegin = true) {
    let newAddons;
    if (!propsAddons) {
        newAddons = customAddons;
    } else {
        newAddons = [customAddons, propsAddons];
    }
    return newAddons;
}
