
/**
 *
 * @param {number} versionId - id verze
 * @param {number|null} nodeId - data key
 * @param {number|string} routingKey - routing key
 */
export function fundSubNodeRequestsFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        // Spočtení data key - se správným id
        // const store = getSubNodeRegister(getState(), versionId, routingKey);
        //
        // if (!store) {
        //     return;
        // }
        //
        // const dataKey = nodeId;
        // if (store.currentDataKey !== dataKey) { // pokus se data key neschoduje, provedeme fetch
        //     dispatch(fundSubNodeRegisterRequest(versionId, nodeId, routingKey));
        //
        //     if (nodeId !== null) {  // pokud chceme reálně načíst objekt, provedeme fetch
        //         return WebApi.getFundNodeRegister(versionId, nodeId)
        //             .then(json => {
        //                 const newStore = getSubNodeRegister(getState(), versionId, routingKey);
        //                 const newDataKey = newStore.currentDataKey;
        //                 if (newDataKey === dataKey) {   // jen pokud příchozí objekt odpovídá dtům, které chceme ve store
        //                     dispatch(fundSubNodeRegisterReceive(versionId, nodeId, routingKey, json))
        //                 }
        //             })
        //     } else {
        //         // Response s prázdným objektem
        //         dispatch(fundSubNodeRegisterReceive(versionId, nodeId, routingKey, null))
        //     }
        // }
    }
}
