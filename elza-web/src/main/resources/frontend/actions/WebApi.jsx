import AjaxUtils from "../components/AjaxUtils";
import { DEFAULT_LIST_SIZE } from '../constants.tsx'

function getData(data, timeout = 1000) {
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
            resolve(data);
        }, timeout);
    });
}
/**
 * Zavolání webscoket operace na serveru.
 * @param url url
 * @param data data pro poslání
 * @param needResponse true, pokud se má čekat na návratové hodnoty ze serveru (včetně chybových stavů), v tuto chvíli chceme vždy
 * @return {Promise}
 */
function callWS(url, data, needResponse = true) {
    return new Promise((resolve, reject) => {
        if (needResponse) { // chceme skoro vždy
            window.ws.send('/app' + url, JSON.stringify(data), (successResponse) => {
                resolve(successResponse);
            }, (errorResponse) => { // příprava pro budoucí možnost odchytávání klientských výjimek - zavolá se error calbback
                reject(errorResponse);
            });
        } else {
            window.ws.send('/app' + url, JSON.stringify(data));
            resolve();
        }
    });
}

/**
 * Web api pro komunikaci se serverem.
 */
export class WebApiCls {

    static baseUrl = '/api';
    static arrangementUrl = WebApiCls.baseUrl + '/arrangement';
    static issueUrl = WebApiCls.baseUrl + '/issue';
    static registryUrl = WebApiCls.baseUrl + '/registry';
    static partyUrl = WebApiCls.baseUrl + '/party';
    static importUrl = WebApiCls.baseUrl + '/import';
    static exportUrl = WebApiCls.baseUrl + '/export';
    static actionUrl = WebApiCls.baseUrl + '/action';
    static kmlUrl = WebApiCls.baseUrl + '/kml';
    static ruleUrl = WebApiCls.baseUrl + '/rule';
    static changesUrl = WebApiCls.arrangementUrl + '/changes';
    static dmsUrl = WebApiCls.baseUrl + '/dms';
    static attachmentUrl = WebApiCls.baseUrl + '/attachment';
    static userUrl = WebApiCls.baseUrl + '/user';
    static groupUrl = WebApiCls.baseUrl + '/group';
    static adminUrl = WebApiCls.baseUrl + '/admin';
    static validateUrl = WebApiCls.baseUrl + '/validate';
    static structureUrl = WebApiCls.baseUrl + '/structure';

    findInFundTree(versionId, nodeId, searchText, type, searchParams = null, luceneQuery = false) {
        const data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
            searchParams: searchParams,
            luceneQuery: luceneQuery
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fulltext', null, data);
    }

    /**
     * Seznam AS serazeny podle poctu vyhledanych JP.
     * Vysledek vyhledavani je ulozeny v user session pro pouziti v {@link #fundFulltext(number)}.
     *
     * @param input vstupni data pro fultextove vyhledavani
     * @return seznam AS razeny podle poctu vyhledanych JP
     */
    fundFulltext(filterText) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundFulltext', null, filterText);
    }

    /**
     * Seznam uzlu daneho AS serazeny podle relevance pri vyhledani.

     * @param fundId identifikátor AS
     * @return seznam uzlu daneho AS serazeny podle relevance pri vyhledani
     */
    fundFulltextNodes(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + `/fundFulltext/${fundId}`, null, fundId);
    }

    getFundsByVersionIds(versionIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getVersions', null, { ids: versionIds });
    }

    getNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodes', null, { versionId: versionId, ids: nodeIds });
    }

    createRelation(relation) {
        return AjaxUtils.ajaxPost(WebApiCls.partyUrl + '/relation', null, relation);
    }

    updateRelation(relation) {
        return AjaxUtils.ajaxPut(WebApiCls.partyUrl + '/relation/' + relation.id, null, relation);
    }

    deleteRelation(relationId) {
        return AjaxUtils.ajaxDelete(WebApiCls.partyUrl + '/relation/' + relationId);
    }

    copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/copyOlderSiblingAttribute/', { versionId, descItemTypeId }, { id: nodeId, version: nodeVersionId });
    }

    createParty(party) {
        return AjaxUtils.ajaxPost(WebApiCls.partyUrl + '/', null, party);
    }

    getParty(partyId) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/' + partyId);
    }

    findParty(search = null, versionId = null, partyTypeId = null, itemSpecId = null, from = 0, count = DEFAULT_LIST_SIZE, scopeId, excludeInvalid) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/', { search, from, count, partyTypeId, versionId, itemSpecId, scopeId, excludeInvalid });
    }

    findPartyUsage(partyId) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/' + partyId + '/usage');
    }

    findPartyForParty(partyId, search = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/findPartyForParty', { search, from, count, partyId });
    }

    updateParty(party) {
        return AjaxUtils.ajaxPut(WebApiCls.partyUrl + '/' + party.id, null, party);
    }

    replaceParty(partyReplaceId, partyReplacementId) {
        return AjaxUtils.ajaxPost(WebApiCls.partyUrl + '/' + partyReplaceId + '/replace', null, partyReplacementId);
    }

    setValidParty(partyId) {
        return AjaxUtils.ajaxPost(WebApiCls.partyUrl + '/' + partyId + '/valid');
    }

    deleteParty(partyId) {
        return AjaxUtils.ajaxDelete(WebApiCls.partyUrl + '/' + partyId);
    }

    findChanges(versionId, nodeId, offset, maxSize, changeId) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId, { nodeId, offset, maxSize, changeId });
    }

    findChangesByDate(versionId, nodeId, changeId, fromDate) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + "/date", { nodeId, maxSize: 1, changeId, fromDate });
    }

    revertChanges(versionId, nodeId, fromChangeId, toChangeId) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + '/revert', { nodeId, fromChangeId, toChangeId });
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet(WebApiCls.validateUrl + '/unitDate', { value: value || '' });
    }

    moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelUnder', null, data)
    }

    moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelBefore', null, data)
    }

    moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelAfter', null, data)
    }

    deleteName(nameId) {
        return AjaxUtils.ajaxDelete(WebApiCls.partyUrl + '/deleteName', { nameId: nameId });
    }

    createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/' + descItemTypeId + '/create', null, descItem);
    }

    createOutputItem(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/' + descItemTypeId + '/create', null, descItem);
    }

    updateDescItem(versionId, nodeVersionId, descItem) {
        return callWS('/arrangement/descItems/' + versionId + '/' + nodeVersionId + '/update/true', descItem);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeVersionId + '/update/true', null,  descItem);
    }

    updateDescItems(fundVersionId, nodeId, nodeVersionId, createDescItem = [], updateDescItem = [], deleteDescItem = []) {
        const changeItems = [];

        createDescItem.forEach(item => {
            changeItems.push({
                updateOp: "CREATE",
                item: item
            });
        });

        updateDescItem.forEach(item => {
            changeItems.push({
                updateOp: "UPDATE",
                item: item
            });
        });

        deleteDescItem.forEach(item => {
            changeItems.push({
                updateOp: "DELETE",
                item: item
            });
        });

        return callWS('/arrangement/descItems/' + fundVersionId + '/' + nodeId + '/' + nodeVersionId + '/update/bulk', changeItems, true);
    }

    setNotIdentifiedDescItem(versionId, nodeId, parentNodeVersion, descItemTypeId, descItemSpecId, descItemObjectId) {
        // return callWS('/arrangement/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/set?descItemTypeId=' + descItemTypeId + '&descItemSpecId=' + descItemSpecId + '&descItemObjectId=' + descItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/set', { descItemTypeId, descItemSpecId, descItemObjectId });
    }

    unsetNotIdentifiedDescItem(versionId, nodeId, parentNodeVersion, descItemTypeId, descItemSpecId, descItemObjectId) {
        // return callWS('/arrangement/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/unset?descItemTypeId=' + descItemTypeId + '&descItemSpecId=' + descItemSpecId + '&descItemObjectId=' + descItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/unset', { descItemTypeId, descItemSpecId, descItemObjectId });
    }

    updateOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionVersion + '/update/true', null, descItem);
    }

    deleteDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeVersionId + '/delete', null, descItem);
    }

    deleteOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionVersion + '/delete', null, descItem);
    }

    deleteDescItemType(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/' + descItemTypeId, null, null);
    }

    deleteOutputItemType(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/' + descItemTypeId, null, null);
    }

    setNotIdentifiedOutputItem(versionId, outputDefinitionId, outputDefinitionVersion, outputItemTypeId, outputItemSpecId, outputItemObjectId) {
        //return callWS('/arrangement/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/notUndefined/set?outputItemTypeId=' + outputItemTypeId + '&outputItemSpecId=' + outputItemSpecId + '&outputItemObjectId=' + outputItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/notUndefined/set', { outputItemTypeId, outputItemSpecId, outputItemObjectId });
    }

    unsetNotIdentifiedOutputItem(versionId, outputDefinitionId, outputDefinitionVersion, outputItemTypeId, outputItemSpecId, outputItemObjectId) {
        //return callWS('/arrangement/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/notUndefined/unset?outputItemTypeId=' + outputItemTypeId + '&outputItemSpecId=' + outputItemSpecId + '&outputItemObjectId=' + outputItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/notUndefined/unset', { outputItemTypeId, outputItemSpecId, outputItemObjectId });
    }

    switchOutputCalculating(fundVersionId, outputDefinitionId, itemTypeId, strict) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + outputDefinitionId + '/' + fundVersionId + '/' + itemTypeId + '/switch', { strict }, null);
    }

    updateOutputSettings(outputId, outputSettings) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + "/settings", null, { ...outputSettings });
    }

    addNode(node, parentNode, versionId, direction, descItemCopyTypes, scenarioName, createItems) {
        const data = {
            versionId,
            direction,
            staticNodeParent: parentNode,
            staticNode: node,
            descItemCopyTypes,
            scenarioName,
            createItems
        };

        return callWS('/arrangement/levels/add', data);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/levels', null, data);
    }

    copyNodesValidate(targetFundVersionId, sourceFundVersionId, sourceNodes, ignoreRootNodes = false, selectedDirection) {
        const data = {
            targetFundVersionId,
            sourceFundVersionId,
            sourceNodes,
            ignoreRootNodes,
            selectedDirection,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/levels/copy/validate', null, data);
    }

    copyNodes(targetFundVersionId, targetStaticNode, targetStaticNodeParent, sourceFundVersionId, sourceNodes, ignoreRootNodes = false, selectedDirection, filesConflictResolve = null, structuresConflictResolve = null) {
        const data = {
            targetFundVersionId,
            targetStaticNode,
            targetStaticNodeParent,
            sourceFundVersionId,
            sourceNodes,
            ignoreRootNodes,
            selectedDirection,
            filesConflictResolve,
            structuresConflictResolve
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/levels/copy', null, data);
    }

    deleteNode(node, parentNode, version) {
        const data = {
            versionId: version,
            staticNodeParent: parentNode,
            staticNode: node
        };

        return callWS('/arrangement/levels/delete', data);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/levels', null, data);
    }

    getNodeAddScenarios(node, versionId, direction, withGroups = false) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/scenarios', { withGroups: withGroups }, {
            versionId,
            direction,
            node
        });
    }

    getBulkActions(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/' + versionId, null);
    }

    getBulkActionsState(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/states/' + versionId, null);
    }

    getBulkActionsList(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/list/' + versionId, null);
    }

    bulkActionValidate(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/validate/' + versionId, null);
    }

    getBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId, null);
    }

    interruptBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId + '/interrupt', null);
    }

    queueBulkAction(versionId, code) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId, code, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null, nodeIds);
    }

    queuePersistentSortByIds(versionId, code, nodeIds, config) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/persistentSort/' + versionId + '/' + code, null, { nodeIds, ...config });
    }

    versionValidate(versionId, showAll = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersion/' + versionId + '/' + showAll, null)
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersionCount/' + versionId, null)
    }


    getFundPolicy(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/fund/policy/' + fundVersionId, {});
    }

    resetServerCache() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/cache/reset', {});
    }

    /// Registry
    createAccessPoint(name, complement, languageCode, description, typeId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/', null, {
            '@class': 'cz.tacr.elza.controller.vo.ApAccessPointCreateVO',
            name,
            description,
            complement,
            languageCode,
            // local: false,
            scopeId,
            typeId
        });
    }

    createStructuredAccessPoint(name, complement, languageCode, description, typeId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/structured', null, {
            '@class': 'cz.tacr.elza.controller.vo.ApAccessPointCreateVO',
            name,
            description,
            complement,
            languageCode,
            // local: false,
            scopeId,
            typeId
        });
    }

    confirmStructuredAccessPoint(accessPointId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/confirm', null, null);
    }
    migrateAccessPoint(accessPointId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/migrate', null, null);
    }
    changeAccessPointItems(accessPointId, items) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/items', null, items);
    }

    deleteAccessPointItemsByType(accessPointId, itemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId + '/type/' + itemTypeId, null, null);
    }

    deleteAccessPoint(accessPointId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId);
    }

    createAccessPointStructuredName(accessPointId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/name/structured', null);
    }

    changeNameItems(accessPointId, objectId, items) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId + '/items', null, items);
    }

    deleteNameItemsByType(accessPointId, objectId, itemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId + '/type/' + itemTypeId, null, null);
    }

    getAccessPointName(accessPointId, objectId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId);
    }


    findRegistry(search = null, registryParent = null, apTypeId = null, versionId = null, itemSpecId = null, from = 0, count = DEFAULT_LIST_SIZE, scopeId = null, excludeInvalid = true) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/', {
            search,
            from,
            count,
            itemSpecId,
            parentRecordId: registryParent,
            apTypeId,
            versionId,
            scopeId,
            excludeInvalid
        });
    }

    findRegistryUsage(recordId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + recordId + '/usage');
    }

    findRecordForRelation(search = null, roleTypeId = null, partyId = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/findRecordForRelation', {
            search,
            from,
            count,
            roleTypeId: roleTypeId,
            partyId: partyId
        });
    }

    getAccessPoint(accessPointId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId);
    }

    updateAccessPoint(accessPointId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId, null, data);
    }

    changeDescription(accessPointId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/description', null, data);
    }

    replaceRegistry(recordReplaceId, recordReplacementId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + recordReplaceId + '/replace', null, recordReplacementId);
    }

    setValidRegistry(registryId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + registryId + '/valid', null);
    }

    deleteAccessPoint(accessPointId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId);
    }

    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/fundScopes', { versionId });
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/scopes', null);
    }

    getAllLanguages() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/languages', null);
    }

    getRecordTypesForAdd(partyTypeId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/recordTypesForPartyType', { partyTypeId });
    }

    createAccessPointName(accessPointId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/name', null, data);
    }

    updateAccessPointName(accessPointId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/name', null, data);
    }

    deleteAccessPointName(accessPointId, objectId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId);
    }

    confirmAccessPointStructuredName(accessPointId, objectId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId + '/confirm', null, null);
    }

    setPreferredAccessPointName(accessPointId, objectId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/name/' + objectId + '/preferred');
    }

    getFragment(fragmentId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/fragment/' + fragmentId);
    }

    createFragment(fragmentTypeCode) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/fragment/create/' + fragmentTypeCode);
    }

    deleteFragment(fragmentId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/fragment/' + fragmentId, null, null);
    }

    confirmFragment(fragmentId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/fragment/' + fragmentId + '/confirm', null, null);
    }

    changeFragmentItems(fragmentId, items) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/fragment/' + fragmentId + '/items', null, items);
    }

    deleteFragmentItemsByType(fragmentId, itemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/fragment/' + fragmentId + '/type/' + itemTypeId, null, null);

    }

    getRecordTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/recordTypes');
    }
    // End registry

    getNodeForm(nodeId, versionId) {
        const node = findNodeById(_faRootNode, nodeId);
        const parents = [];
        const siblings = [...node.parent.children];
        let n = node.parent;
        while (n !== null) {
            parents.push(n);
            n = n.parent;
        }

        const data = {
            parents: parents,
            children: node.children,
            siblings: siblings,
        };
        return getData(data, 1);
    }

    getFundNodeForm(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + nodeId + '/' + versionId + '/form');
    }

    getOutputNodeForm(versionId, outputDefinitionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + outputDefinitionId + '/' + versionId + '/form');
    }

    getFundNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + versionId + '/forms', { nodeIds: nodeIds })
    }

    getFundNodeFormsWithAround(versionId, nodeId, around) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms');
    }

    getFundNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeDaos(versionId, nodeId = null, detail = false, from = 0, max = 10000) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daos/' + versionId, {
            nodeId,
            detail,
            index: from,
            maxResults: max,
        });
    }

    findDaoPackages(versionId, search, unassigned) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daopackages/' + versionId, { search, unassigned });
    }

    getPackageDaos(versionId, daoPackageId, unassigned, detail = false, from = 0, max = 10000) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daos/' + versionId + "/" + daoPackageId, {
            detail,
            index: from,
            maxResults: max,
            unassigned
        });
    }

    deleteFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/delete', null, data);
    }

    createFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/create', null, data);
    }

    updateFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/update', null, data);
    }

    getRulDataTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/dataTypes');
    }

    getDescItemTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/descItemTypes')
    }

    getGroups(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/groups/' + fundVersionId);
    }

    getCalendarTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/calendarTypes');
    }

    getTemplates(code = null) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/templates', code ? { code } : null);
    }


    getFundNodeForm1(versionId, nodeId) {
        const node = findNodeById(_faRootNode, nodeId);
        const data = {
            node: node,
            data: {
                groups: [
                    {
                        name: 'group 1',
                        attrDesc: [
                            { id: 1, name: 'Ref. ozn.', multipleValue: false, code: 'STRING', values: [{ value: '' }], width: 1 },
                            { id: 2, name: 'Obsah/regest', multipleValue: false, code: 'TEXT', values: [{ value: '' }], width: 4 },
                            { id: 4, name: 'Typ archiválie', multipleValue: false, code: 'STRING', values: [{ value: '' }], width: 1 },
                            { id: 3, name: 'Ukládací jednotka', multipleValue: false, code: 'STRING', values: [{ value: '' }], width: 1 },
                            { id: 5, name: 'Datace', multipleValue: false, code: 'STRING', values: [{ value: '' }], width: 1 },
                        ]
                    },
                ]
            }
        };
        return getData(data, 1);
    }

    getRequestsInQueue() {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/queued');
    }

    deleteRequestFromQueue(id) {
        return getData({}, 100);
    }

    findRequests(versionId, type, state, description, fromDate, toDate, subType) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId, {
            state,
            type,
            description,
            fromDate,
            toDate,
            subType
        });
    }

    arrDigitizationRequestAddNodes(versionId, reqId, send, description, nodeIds, digitizationFrontdeskId) {
        const data = {
            id: reqId,
            nodeIds,
            description,
            digitizationFrontdeskId
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/digitization/add', { send }, data);
    }

    arrDaoRequestAddDaos(versionId, reqId, send, description, daoIds, type) {
        const data = {
            id: reqId,
            daoIds,
            description,
            type
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/dao/add', { send }, data);
    }

    arrRequestRemoveNodes(versionId, reqId, nodeIds) {
        const data = {
            id: reqId,
            nodeIds,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/digitization/remove', null, data);
    }

    updateArrRequest(versionId, id, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id, null, data);
    }

    removeArrRequestQueueItem(id) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getArrRequest(versionId, id) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId + "/" + id, { detail: true });
    }

    sendArrRequest(versionId, id) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + "/" + id + "/send");
    }

    deleteArrRequest(versionId, id) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getFundTree(versionId, nodeId, expandedIds = {}, includeIds = []) {
        const data = {
            versionId,
            nodeId,
            includeIds,
            expandedIds: []
        };
        for (let prop in expandedIds) { // Použít Object.keys()
            if (expandedIds[prop]) {
                data.expandedIds.push(prop);
            }
        }
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundTree', null, data);
    }

    getNodeData(fundVersionId, nodeParam, resultParam = {}) {
        const data = {
            fundVersionId: fundVersionId,
            nodeId: nodeParam.nodeId,
            nodeIndex: nodeParam.nodeIndex,
            parentNodeId: nodeParam.parentNodeId,
            formData: resultParam.formData,
            siblingsFrom: resultParam.siblingsFrom,
            siblingsMaxCount: resultParam.siblingsMaxCount,
            siblingsFilter: resultParam.siblingsFilter,
            parents: resultParam.parents,
            children: resultParam.children,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodeData', null, data);
    }

    getFundTreeNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundTree/nodes', null, {
            versionId,
            nodeIds
        });
    }

    getPartyNameFormTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/partyNameFormTypes');
    }

    getPartyTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/partyTypes');
    }

    getExternalSystemsSimple() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems/simple');
    }

    getRuleSets() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/getRuleSets');
    }

    createFund(createFund) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/funds', null, createFund);
    }

    updateFund(data) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/updateFund', { ruleSetId: data.ruleSetId }, data)
    }

    approveVersion(versionId, dateRange) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/approveVersion', { dateRange, versionId });
    }

    filterNodes(versionId, filter) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/filterNodes/' + versionId, {}, filter)
    }

    getFilteredNodes(versionId, pageIndex, pageSize, descItemTypeIds) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/getFilterNodes/' + versionId, { page: pageIndex, pageSize: pageSize }, descItemTypeIds)
    }

    replaceDataValues(versionId, descItemTypeId, specsIds, searchText, replaceText, nodes, selectionType) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/replaceDataValues/' + versionId, { descItemTypeId, searchText, replaceText }, { nodes, specIds: specsIds, selectionType })
    }

    placeDataValues(versionId, descItemTypeId, specsIds, replaceText, replaceSpecId, nodes, selectionType) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/placeDataValues/' + versionId, { descItemTypeId, newDescItemSpecId: replaceSpecId, text: replaceText }, { nodes, specIds: specsIds, selectionType })
    }

    setSpecification(fundVersionId, itemTypeId, specIds, replaceSpecId, nodes, selectionType) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/setSpecification/' + fundVersionId, { itemTypeId, replaceSpecId }, { nodes, specIds, selectionType })
    }

    deleteDataValues(versionId, descItemTypeId, specsIds, nodes, selectionType) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/deleteDataValues/' + versionId, { descItemTypeId }, { nodes, specIds: specsIds, selectionType })
    }

    getFilteredFulltextNodes(versionId, fulltext, luceneQuery = false, searchParams = null) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getFilteredFulltext/' + versionId, null, { fulltext, luceneQuery, searchParams })
    }

    getPackages() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/getPackages');
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/deletePackage/' + code);
    }

    createDaoLink(versionId, daoId, nodeId) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/daos/' + versionId + "/" + daoId + "/" + nodeId + "/create", null, null);
    }

    deleteDaoLink(versionId, daoLinkId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/daolinks/' + versionId + "/" + daoLinkId, null, null);
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.ruleUrl + '/importPackage', {}, 'POST', data);
    }

    reindex() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/reindex');
    }

    getIndexingState() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/reindexStatus');
    }

    getTransformations() {
        return AjaxUtils.ajaxGet(WebApiCls.importUrl + '/transformations');
    }

    getExportTransformations() {
        return AjaxUtils.ajaxGet(WebApiCls.importUrl + '/transformations');
    }

    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.importUrl + '/import', {}, 'POST', data);
    }

    arrCoordinatesImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);

        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/descCoordinates', {}, 'POST', formData);
    }

    arrOutputCoordinatesImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);

        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/outputCoordinates', {}, 'POST', formData);
    }

    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/regCoordinates', {}, 'POST', data);
    }

    descItemCsvImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApiCls.arrangementUrl + '/descItems/' + versionId + '/csv/import', {}, 'POST', formData);
    }

    descOutputItemCsvImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/csv/import', {}, 'POST', formData);
    }

    getInstitutions() {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/institutions');
    }

    /**
     * Hledá všechny unikátní hodnoty atributu pro daný AS
     */
    getDescItemTypeValues(versionId, descItemTypeId, fulltext, descItemSpecIds, max) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/filterUniqueValues/' + versionId, { descItemTypeId, fulltext, max }, descItemSpecIds)
    }

    findUniqueSpecIds(fundVersionId, itemTypeId, filters) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/findUniqueSpecIds/' + fundVersionId, { itemTypeId }, filters)
    }

    getVisiblePolicy(nodeId, fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/types');
    }

    setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree = false, nodeExtensions) {
        return AjaxUtils.ajaxPut(WebApiCls.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId, null, { policyTypeIdsMap, includeSubtree, nodeExtensions });
    }

    getUserDetail() {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/detail');
    }

    setUserSettings(settings) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/detail/settings', null, settings);
    }

    login(username, password) {
        return AjaxUtils.ajaxCallRaw('/login', {}, 'POST', 'username=' + username + '&password=' + password, 'application/x-www-form-urlencoded');
    }

    logout() {
        return AjaxUtils.ajaxCallRaw('/logout', {}, 'POST', '', 'application/x-www-form-urlencoded', true);
    }

    findFunds(fulltext, max = DEFAULT_LIST_SIZE, from = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFunds', { fulltext, max, from })
            .then(json => ({ funds: json.list, fundCount: json.count, max, from }))
    }

    findUser(fulltext, active, disabled, max = DEFAULT_LIST_SIZE, groupId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '', { search: fulltext, active, disabled, from: 0, count: max, excludedGroupId: groupId })
            .then(json => ({ users: json.rows, usersCount: json.count }))
    }

    findControlFunds(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/controlFunds', { search: fulltext, from: 0, count: max });
    }

    findUserWithFundCreate(fulltext, active, disabled, max = DEFAULT_LIST_SIZE, groupId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + "/withFundCreate", { search: fulltext, active, disabled, from: 0, count: max, excludedGroupId: groupId })
            .then(json => ({ users: json.rows, usersCount: json.count }))
    }

    findUsersPermissionsByFund(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + `/fund/${fundId}/users`)
            .then(data => ({ rows: data, count: data.length }));
    }

    findUsersPermissionsByFundAll() {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + `/fund/all/users`)
            .then(data => ({ rows: data, count: data.length }));
    }

    findGroupsPermissionsByFund(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/${fundId}/groups`)
            .then(data => ({ rows: data, count: data.length }));
    }

    findGroupsPermissionsByFundAll(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/all/groups`)
            .then(data => ({ rows: data, count: data.length }));
    }

    changeUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission', null, permissions);
    }

    addUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission/add', null, permissions);
    }

    addGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + "/" + groupId + '/permission/add', null, permissions);
    }

    deleteUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission/delete', null, permissions);
    }

    deleteGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + "/" + groupId + '/permission/delete', null, permissions);
    }

    deleteUserFundPermission(userId, fundId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission/delete/fund/' + fundId);
    }

    deleteUserFundAllPermission(userId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission/delete/fund/all');
    }

    deleteGroupFundPermission(groupId, fundId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + "/" + groupId + '/permission/delete/fund/' + fundId);
    }

    deleteGroupFundAllPermission(groupId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + "/" + groupId + '/permission/delete/fund/all');
    }

    deleteUserScopePermission(userId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/" + userId + '/permission/delete/scope/' + scopeId);
    }

    deleteGroupScopePermission(groupId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + "/" + groupId + '/permission/delete/scope/' + scopeId);
    }

    changeGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + "/group/" + groupId + '/permission', null, permissions);
    }

    findGroup(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl, { search: fulltext, from: 0, count: max })
            .then(json => ({ groups: json.rows, groupsCount: json.count }))
    }

    findGroupWithFundCreate(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + "/withFundCreate", { search: fulltext, from: 0, count: max })
            .then(json => ({ groups: json.rows, groupsCount: json.count }))
    }

    getUser(userId) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId);
    }

    getUserOld(userId) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId + "/old");
    }

    createGroup(name, code, description) {
        const params = {
            name: name,
            code: code,
            description
        };
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl, null, params);
    }

    updateGroup(groupId, name, description) {
        return AjaxUtils.ajaxPut(WebApiCls.groupUrl + '/' + groupId, null, { name, description });
    }

    deleteGroup(groupId) {
        return AjaxUtils.ajaxDelete(WebApiCls.groupUrl + '/' + groupId);
    }

    joinGroup(groupIds, userIds) {
        const data = {
            groupIds: groupIds,
            userIds: userIds
        };
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/join/', null, data);
    }

    leaveGroup(groupId, userId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/' + groupId + '/leave/' + userId, null, null);
    }

    createUser(username, password, partyId) {
        const params = {
            username: username,
            password: password,
            partyId: partyId
        }
        return AjaxUtils.ajaxPost(WebApiCls.userUrl, null, params);
    }

    updateUser(id, username, password) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + id, null, { username, password });
    }

    changePasswordUser(oldPassword, newPassword) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/password', null, { oldPassword, newPassword });
    }

    changePassword(userId, newPassword) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/password', null, { newPassword });
    }

    changeActive(userId, active) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/active/' + active);
    }

    getGroup(groupId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + '/' + groupId);
    }

    getFundDetail(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFund/' + fundId)
            .then(json => {
                return {
                    ...json,
                    versionId: json.versions[0].id,
                    activeVersion: json.versions[0],
                }
            })
    }

    getValidationItems(fundVersionId, fromIndex, toIndex) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/' + fromIndex + '/' + toIndex);
    }

    findValidationError(fundVersionId, nodeId, direction) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/find/' + nodeId + '/' + direction);
    }

    deleteFund(fundId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/deleteFund/' + fundId);
    }

    getOutputTypes(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/types/' + versionId);
    }

    getOutputs(versionId, state) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + versionId + (state != null ? '?state=' + state : ''));
    }

    getFundOutputDetail(versionId, outputId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createOutput(versionId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + versionId, null, data);
    }

    updateOutput(versionId, outputId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/update', null, data);
    }

    outputUsageEnd(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/lock');
    }

    fundOutputAddNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/add', null, nodeIds);
    }

    fundOutputRemoveNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/remove', null, nodeIds);
    }

    outputDelete(versionId, outputId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createFundFileRaw(formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund/', {}, 'POST', formData);
    }

    createFundFile(formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund', null, formData);
    }

    getMimeTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.attachmentUrl + '/mimeTypes', null);
    }

    findFundFiles(fundId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId, { 'count': count, 'search': searchText });
    }

    getEditableFundFile(fundId, fileId) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId + "/" + fileId);
    }

    updateFundFileRaw(fileId, formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund/' + fileId, {}, 'POST', formData);
    }

    updateFundFile(fileId, formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund/' + fileId, null, formData);
    }

    deleteArrFile(fileId) {
        return AjaxUtils.ajaxDelete(WebApiCls.dmsUrl + '/fund/' + fileId, null, null);
    }

    findFundOutputFiles(resultId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/output/' + resultId, { 'count': count, 'search': searchText });
    }

    getFundOutputFunctions(outputId, getRecommended) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/output/' + outputId, { 'recommended': getRecommended });
    }

    outputGenerate(outputId, forced = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/generate/' + outputId, { forced });
    }

    outputRevert(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/revert');
    }

    outputClone(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/clone');
    }

    getApExternalSystems() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/externalSystems');
    }

    getEidTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/eidTypes');
    }

    findInterpiRecords(criteria) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/interpi', null, criteria);
    }

    importRecord(importVO) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/interpi/import/', null, importVO);
    }

    importRecordUpdate(recordId, importVO) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/interpi/import/' + recordId, null, importVO);
    }

    findInterpiRecordRelations(recordId, relationsVO) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/interpi/' + recordId + '/relations/', null, relationsVO);
    }

    getAllExtSystem() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems', null)
    }

    getExtSystem(id) {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems/' + id, null)
    }

    createExtSystem(extSystem) {
        return AjaxUtils.ajaxPost(WebApiCls.adminUrl + '/externalSystems', null, extSystem);
    }

    updateExtSystem(id, extSystem) {
        return AjaxUtils.ajaxPut(WebApiCls.adminUrl + '/externalSystems/' + id, null, extSystem);
    }

    deleteExtSystem(id) {
        return AjaxUtils.ajaxDelete(WebApiCls.adminUrl + '/externalSystems/' + id, null);
    }

    specificationHasParty(itemSpecId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/specificationHasParty/' + itemSpecId);
    }

    findFundStructureExtension(fundVersionId, structureTypeCode) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode);
    }

    updateFundStructureExtension(fundVersionId, structureTypeCode, structureExtensionCodes) {
        return AjaxUtils.ajaxPut(WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode, null, structureExtensionCodes);
    }

    findRulStructureTypes(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/type', { fundVersionId });
    }

    getStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    findStructureData(fundVersionId, structureTypeCode, search = null, assignable = true, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureTypeCode + '/search', {
            search,
            assignable,
            from,
            count
        });
    }

    createStructureData(fundVersionId, structureTypeCode, value = null) {
        // Kvůli JSON stringify musíme poslat pomocí RAW aby se nevytvořili '"' v body
        return AjaxUtils.ajaxCallRaw(WebApiCls.structureUrl + '/data/' + fundVersionId, { value }, "POST", structureTypeCode, 'application/json');
    }

    duplicateStructureDataBatch(fundVersionId, structureDataId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/batch', null, data);
    }

    confirmStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/confirm');
    }

    deleteStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxDelete(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    getFormStructureItems(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/item/form/' + fundVersionId + '/' + structureDataId);
    }


    createStructureItem(fundVersionId, structureDataId, itemTypeId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/item/' + fundVersionId + '/' + structureDataId + '/' + itemTypeId + '/create', null, data)
    }

    updateStructureItem(fundVersionId, data, createNewVersion = true) {
        return AjaxUtils.ajaxPut(WebApiCls.structureUrl + '/item/' + fundVersionId + '/update/' + createNewVersion, null, data)
    }

    deleteStructureItem(fundVersionId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/item/' + fundVersionId + '/delete', null, data)
    }

    deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.structureUrl + '/item/' + fundVersionId + '/' + structureDataId + '/' + itemTypeId)
    }

    updateStructureDataBatch(fundVersionId, structureTypeCode, structureDataBatchUpdate) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureTypeCode + '/batchUpdate', null, structureDataBatchUpdate);
    }

    setAssignableStructureDataList(fundVersionId, assignable, structureDataIds) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/data/' + fundVersionId + '/assignable/' + assignable, null, structureDataIds)
    }

    /**
     * Získání druhů připomnek.
     *
     * @returns {Promise} list druhů připomínek
     */
    findAllIssueTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_types');
    }

    /**
     * Získání stavů připomínek.
     *
     * @returns {Promise} list stavů připomínek
     */
    findAllIssueStates(): IssueStateVO[] {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_states');
    }

    /**
     * Získání protokolů pro konkrétní archivní souboru.
     *
     * @param fundId identifikátor AS
     * @param open filter zda je issue list otevřen nebo zavřen
     * @returns {Promise} seznam protokolů
     */
    findIssueListByFund(fundId: number, open: boolean = null) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/funds/' + fundId + '/issue_lists', { open });
    }

    /**
     * Získání detailu protokolu.
     *
     * @param issueListId identifikátor protokolu.
     * @returns {Promise} detail protokolu
     */
    getIssueList(issueListId: number): IssueListVO {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_lists/' + issueListId);
    }

    /**
     * Získání seznam připomínek dle parametrů.
     *
     * @param issueListId identifikátor protokolu.
     * @param issueStateId identifikátor stavu připomínky dle kterého filtrujeme
     * @param issueTypeId identifikátor druhu připomínky dle kterého filtrujeme
     * @returns {Promise} seznam připomínek
     */
    findIssueByIssueList(issueListId: number, issueStateId: number = null, issueTypeId: number = null) {
        const requestParams = {
            issueStateId,
            issueTypeId
        };
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_lists/' + issueListId + '/issues', requestParams);
    }

    /**
     * Založení nového protokolu.
     *
     * @param data {IssueListVO} data pro založení protokolu
     */
    addIssueList(data: IssueListVO): IssueListVO {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issue_lists', null, data)
    }

    /**
     * Úprava vlastností existujícího protokolu
     *
     * @param issueListId identifikátor protokolu.
     * @param data {IssueListVO} data pro uložení protokolu
     */
    updateIssueList(issueListId: number, data: IssueListVO): IssueListVO {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/issue_lists/' + issueListId, null, data)
    }

    /**
     * Získání detailu připomínky.
     *
     * @param issueId identifikátor připomínky
     * @returns {Promise} detail připomínky
     */
    getIssue(issueId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issues/' + issueId);
    }

    /**
     * Přidání připomínky k protokolu.
     *
     * @param data {IssueVO} data pro přidání připomínky
     * @returns {Promise}
     */
    addIssue(data: IssueVO) {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issues', null, data)
    }

    /**
     * Úprava připomínky.
     *
     * @param issueId identifikátor připomínky
     * @param data {IssueVO} data pro uložení připomínky
     */
    updateIssue(issueId: number, data: IssueVO) {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/issues/' + issueId, null, data)
    }

    /**
     * Změna druhu připomínky.
     *
     * @param issueId     identifikátor připomínky
     * @param issueTypeId identifikátor stavu připomínky
     * @returns {Promise}
     */
    setIssueType(issueId: number, issueTypeId: number) {
        const requestParams = {
            issueTypeId,
        };
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issues/' + issueId + '/type', requestParams)
    }

    /**
     * Vyhledání komentářů k připomínce.
     *
     * @param issueId identifikátor připomínky
     * @returns {Promise} pole {CommentVO}
     */
    findIssueCommentByIssue(issueId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issues/' + issueId + '/comments');
    }

    /**
     * Získání detailu komentáře.
     *
     * @param commentId identifikátor komentáře
     * @returns {Promise} detail {CommentVO}
     */
    getIssueComment(commentId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/comments/' + commentId);
    }

    /**
     * Založení nového komentáře.
     *
     * @param data komentář
     * @returns {Promise}
     */
    addIssueComment(data: CommentVO) {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/comments', null, data)
    }

    /**
     * Úprava komentáře.
     *
     * @param commentId identifikátor komentáře
     * @param data komentář
     * @returns {Promise}
     */
    updateIssueComment(commentId: number, data: CommentVO) {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/comments/' + commentId, null, data)
    }

    /**
     * Vyhledá další uzel s otevřenou připomínkou.
     *
     * @param fundVersionId verze AS
     * @param nodeId výchozí uzel (default root)
     * @param direction krok (default 1)
     */
    nextIssueByFundVersion(fundVersionId: number, nodeId: number, direction: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/funds/' + fundVersionId + '/issues/nextNode', { nodeId, direction });
    }
}

declare class IssueListVO extends Object {
    id: number;
    fundId: number;
    name: string;
    open: boolean;
    rdUsers: UsrUserVO[];
    wrUsers: UsrUserVO[];
}

declare class IssueStateVO extends Object {
    id: number;
    code: string;
    name: string;
    startState: boolean;
    finalState: boolean;
}

declare class IssueVO extends Object {
    id: number;
    issueListId: number;
    nodeId: number;
    number: number;
    issueTypeId: number;
    issueStateId: number;
    description: string;
    userCreate: UsrUserVO;
    timeCreated: string;
    referenceMark: string[];
}

declare class CommentVO extends Object {
    id: number;
    issueId: number;
    comment: string;
    user: UsrUserVO;
    prevStateId: number;
    nextStateId: number;
    timeCreated: string;
}

declare class UsrUserVO extends Object {
    username: string;
    id: string;
    active: boolean;
    description: string;
    party: Object;
    permissions: Object[];
    groups: Object[];
}

/**
 * Továrna URL
 *
 * Jednoduché statické metody vracející pouze String - URL
 */
export class UrlFactory {
    static exportPackage(code) {
        return serverContextPath + WebApiCls.ruleUrl + '/exportPackage/' + code;
    }

    static exportFund() {
        return serverContextPath + WebApiCls.exportUrl + '/create';
    }

    static exportRegCoordinate(objectId) {
        return serverContextPath + WebApiCls.kmlUrl + '/export/regCoordinates/' + objectId;
    }

    static exportArrCoordinate(objectId, versionId) {
        return serverContextPath + WebApiCls.kmlUrl + '/export/descCoordinates/' + versionId + '/' + objectId;
    }

    static exportItemCsvExport(objectId, versionId, typePrefix) {
        return serverContextPath + WebApiCls.arrangementUrl + '/' + typePrefix + 'Items/' + versionId + '/csv/export?descItemObjectId=' + objectId
    }

    static downloadDmsFile(id) {
        return serverContextPath + WebApiCls.dmsUrl + '/' + id
    }

    static downloadGeneratedDmsFile(id, fundId, mimeType) {
        return serverContextPath + WebApiCls.dmsUrl + `/fund/${fundId}/${id}/generated?mimeType=${mimeType}`;
    }

    static downloadOutputResult(id) {
        return serverContextPath + '/api/outputResult/' + id
    }

    static exportIssueList(issueListId) {
        return serverContextPath + WebApiCls.issueUrl + `/issue_lists/${issueListId}/export`;
    }
}
/**
 * Class that overrides the original WebApiCls and replaces them with methods,
 * that postpone requests, when user is not logged in (unauthorized)
 */
export class WebApiOverride extends WebApiCls {
    constructor() {
        super();
        this.callbacks = [];
        // get all method names from WebApiCls
        this.origMethodNames = Object.getOwnPropertyNames(WebApiCls.prototype);
        this.overrideMethods();
    }
    /**
     * Overrides the old WebApi methods with new
     */
    overrideMethods() {
        const { origMethodNames } = this;

        for (const i in origMethodNames) {
            const methodName = origMethodNames[i];
            const origMethod = this[methodName];

            this[methodName] = (...args) => {
                return this.newMethod(origMethod, args);
            }
        }
    }
    /**
     * Creates new WebApi method, which postpones the requests that failed, due to user being unauthorized
     */
    newMethod(origMethod, args) {
        return new Promise((resolve, reject) => {
            origMethod.call(this, ...args).then((json) => {
                resolve(json);
            }).catch((err) => {
                if (err.unauthorized) {
                    this.callbacks.push(() => {
                        origMethod.call(this, ...args).then(resolve).catch(reject);
                    });
                } else {
                    reject(err);
                }
            });
        });
    }
    /**
     * Repeats all postponed requests
     */
    onLogin() {
        if (this.callbacks && this.callbacks.length > 0) {
            this.callbacks.forEach(callback => callback());
            this.callbacks = [];
        }
    }
}

export const WebApi = new WebApiOverride();
export const _WebApi = new WebApiCls();

// export default {
//     WebApi: new WebApi(),
//     WebApiCls: WebApi,;
//     UrlFactory: UrlFactory,
// };
