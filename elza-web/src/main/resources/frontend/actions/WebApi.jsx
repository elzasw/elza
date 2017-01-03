import {AjaxUtils} from 'components/index.jsx';
import {DEFAULT_LIST_SIZE} from 'constants'

function getData(data, timeout = 1000) {
    return new Promise(function (resolve, reject) {
        setTimeout(function() {
            resolve(data);
        }, timeout);
    });
}

const digReqs = [
    {id: 1, code: "026c75c4-4ee7-4f7f-9c91-49df8e5abbcf", filesCount: 1, label: "Fotka - Petr Compel", url: "http://info.marbes.cz/modules/obrazky/preved_fotku.php?id=562"},
    {id: 2, code: "09a2301a-61b9-4ce5-a013-df6e4f23a7bd", filesCount: 0, label: "Fotka - Michal Moučka", url: "http://info.marbes.cz/modules/obrazky/preved_fotku.php?id=55"},
    {id: 3, code: "5369d629-baa0-4571-b5df-0c339f8fa36f", filesCount: 2, label: "Fotka - Martin Šlapa"},
    {id: 4, code: "5369d629-baa0-4570-b5df-0c339f8fa36f", filesCount: 8, label: "Fotka - Václav Mařík", url: "http://info.marbes.cz/modules/obrazky/preved_fotku.php?id=54"},
];

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
            window.ws.send(serverContextPath + '/app' + url, {}, JSON.stringify(data), (successResponse) => {
                resolve(successResponse);
            }, (errorResponse) => { // příprava pro budoucí možnost odchytávání klientských výjimek - zavolá se error calbback
                reject(errorResponse);
            });
        } else {
            window.ws.send(serverContextPath + '/app' + url, {}, JSON.stringify(data));
            resolve();
        }
    });
}

/**
 * Továrna URL
 *
 * Jednoduché statické metody vracející pouze String - URL
 */
class UrlFactory {
    static exportPackage(code) {
        return WebApi.ruleUrl + '/exportPackage/' + code;
    }

    static exportFund(versionId, transformationName) {
        return WebApi.exportUrl + '/fund/' + versionId + '?transformationName=' + encodeURIComponent(transformationName);
    }

    static exportRegCoordinate(objectId) {
        return WebApi.kmlUrl + '/export/regCoordinates/' + objectId;
    }

    static exportArrCoordinate(objectId, versionId) {
        return window.location.origin + WebApi.kmlUrl + '/export/descCoordinates/' + versionId + '/' + objectId;
    }

    static exportItemCsvExport(objectId, versionId, typePrefix) {
        return window.location.origin + WebApi.arrangementUrl + '/' + typePrefix + 'Items/' + versionId + '/csv/export?descItemObjectId=' + objectId
    }

    static downloadDmsFile(id) {
        return window.location.origin + WebApi.dmsUrl + '/' + id
    }
    static downloadOutputResult(id) {
        return window.location.origin + '/api/outputResult/' + id
    }
}

/**
 * Web api pro komunikaci se serverem.
 */
class WebApi {
    constructor() {
    }

    findInFundTree(versionId, nodeId, searchText, type) {
        const data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
        };
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fulltext', null,  data);
    }

    getFundsByVersionIds(versionIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/getVersions', null, {ids: versionIds});
    }

    getNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/nodes', null, {versionId: versionId, ids: nodeIds});
    }

    createRelation(relation) {
        return AjaxUtils.ajaxPost(WebApi.partyUrl + '/relation', null,  relation);
    }

    updateRelation(relation) {
        return AjaxUtils.ajaxPut(WebApi.partyUrl + '/relation/' + relation.id, null,  relation);
    }

    deleteRelation(relationId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/relation/' + relationId);
    }

    copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/copyOlderSiblingAttribute/', {versionId, descItemTypeId},  {id: nodeId, version: nodeVersionId});
    }

    createParty(party) {
        return AjaxUtils.ajaxPost(WebApi.partyUrl + '/', null, party);
    }

    getParty(partyId){
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/' + partyId);
    }

    findParty(search = null, versionId = null, partyTypeId = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/', {search, from, count, partyTypeId, versionId});
    }

    findPartyForParty(partyId, search = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/findPartyForParty', {search, from, count, partyId});
    }

    updateParty(party) {
        return AjaxUtils.ajaxPut(WebApi.partyUrl + '/' + party.id, null, party);
    }

    deleteParty(partyId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/' + partyId);
    }

    findChanges(versionId, nodeId, offset, maxSize, changeId){
        return AjaxUtils.ajaxGet(WebApi.changesUrl + '/' + versionId, {nodeId, offset, maxSize, changeId});
    }

    findChangesByDate(versionId, nodeId, changeId, fromDate) {
        return AjaxUtils.ajaxGet(WebApi.changesUrl + '/' + versionId + "/date", {nodeId, maxSize: 1, changeId, fromDate});
    }

    revertChanges(versionId, nodeId, fromChangeId, toChangeId){
        return AjaxUtils.ajaxGet(WebApi.changesUrl + '/' + versionId + '/revert', {nodeId, fromChangeId, toChangeId});
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet(WebApi.validateUrl + '/unitDate', {value: value || ''});
    }

    moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelUnder', null, data)
    }

    moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelBefore', null, data)
    }

    moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        };
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelAfter', null, data)
    }

    deleteName(nameId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/deleteName', {nameId: nameId});
    }

    createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/' + descItemTypeId + '/create', null,  descItem);
    }

    createOutputItem(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/' + descItemTypeId + '/create', null,  descItem);
    }

    updateDescItem(versionId, nodeVersionId, descItem) {
        return callWS('/arrangement/descItems/' + versionId + '/' + nodeVersionId + '/update/true', descItem);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeVersionId + '/update/true', null,  descItem);
    }

    updateOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionVersion + '/update/true', null,  descItem);
    }

    deleteDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeVersionId + '/delete', null,  descItem);
    }

    deleteOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionVersion + '/delete', null,  descItem);
    }

    deleteDescItemType(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/' + descItemTypeId, null, null);
    }

    deleteOutputItemType(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/outputItems/' + versionId + '/' + outputDefinitionId + '/' + outputDefinitionVersion + '/' + descItemTypeId, null, null);
    }

    switchOutputCalculating(fundVersionId, outputDefinitionId, itemTypeId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + outputDefinitionId + '/' + fundVersionId + '/' + itemTypeId + '/switch', null, null);
    }

    addNode(node, parentNode, versionId, direction, descItemCopyTypes, scenarioName) {
        const data = {
            versionId,
            direction,
            staticNodeParent: parentNode,
            staticNode: node,
            descItemCopyTypes,
            scenarioName
        };

        return callWS('/arrangement/levels/add', data);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/levels', null, data);
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
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/scenarios', {withGroups: withGroups}, {
            versionId,
            direction,
            node
        });
    }

    getBulkActions(versionId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/' + versionId, null);
    }

    getBulkActionsState(versionId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/states/' + versionId, null);
    }

    getBulkActionsList(versionId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/list/' + versionId, null);
    }

    bulkActionValidate(versionId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/validate/' + versionId, null);
    }

    getBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/action/' + bulkActionRunId, null);
    }

    interruptBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/action/' + bulkActionRunId + '/interrupt', null);
    }

    queueBulkAction(versionId, code) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId, code, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.actionUrl + '/queue/' + versionId + '/' + code, null, nodeIds);
    }

    versionValidate(versionId, showAll = false) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validateVersion/' + versionId + '/' + showAll, null)
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validateVersionCount/' + versionId, null)
    }


    getFundPolicy(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/fund/policy/' + fundVersionId, {});
    }

    resetServerCache() {
        return AjaxUtils.ajaxGet(WebApi.adminUrl + '/cache/reset', {});
    }

    /// Registry
    createRecord(record, characteristics, registerTypeId, parentId, scopeId) {
        return AjaxUtils.ajaxPost(WebApi.registryUrl + '/', null, {
            '@class': 'cz.tacr.elza.controller.vo.RegRecordVO',
            record,
            characteristics,
            local: false,
            scopeId,
            parentRecordId: parentId,
            registerTypeId
        });
    }

    findRegistry(search = null, registryParent = null, registerTypeId = null, versionId = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/', {
            search,
            from,
            count,
            parentRecordId: registryParent,
            registerTypeId: registerTypeId,
            versionId
        });
    }

    findRecordForRelation(search = null, roleTypeId = null, partyId = null, from = 0, count = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/findRecordForRelation',{
            search,
            from,
            count,
            roleTypeId: roleTypeId,
            partyId: partyId
        });
    }

    getRegistry(registryId) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/' + registryId);
    }

    updateRegistry(record) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/' + record.id, null, record);
    }

    deleteRegistry(recordId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/' + recordId);
    }

    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/fundScopes', {versionId});
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/scopes', null);
    }

    getRecordTypesForAdd(partyTypeId = null){
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/recordTypesForPartyType', {partyTypeId});
    }

    addRegistryVariant(data) {
        return AjaxUtils.ajaxPost(WebApi.registryUrl + '/variantRecord/', null, data);
    }

    editRegistryVariant(variantRecord) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/variantRecord/' + variantRecord.id, null, variantRecord);
    }

    deleteVariantRecord(variantRecordId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/variantRecord/' + variantRecordId);
    }

    createRegCoordinates(data){
        return AjaxUtils.ajaxPost(WebApi.registryUrl + '/regCoordinates', null, data);
    }

    updateRegCoordinates(coordinates) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/regCoordinates/' + coordinates.id, null, coordinates);
    }

    deleteRegCoordinates(coordinatesId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/regCoordinates/' + coordinatesId);
    }

    getRecordTypes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/recordTypes');
    }

    getDefaultScopes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/defaultScopes');
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
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodes/' + nodeId + '/' + versionId + '/form');
    }

    getOutputNodeForm(versionId, outputDefinitionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/' + outputDefinitionId + '/' + versionId + '/form');
    }

    getFundNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodes/' + versionId + '/forms', {nodeIds: nodeIds})
    }

    getFundNodeFormsWithAround(versionId, nodeId, around) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms');
    }

    getFundNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeDaos(versionId, nodeId = null, detail = false) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/daos/' + versionId, {nodeId, detail});
        //return getData(digReqs, 200);
    }

    deleteFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/delete', null, data);
    }

    createFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/create', null, data);
    }

    updateFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/update', null, data);
    }

    getRulDataTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/dataTypes');
    }

    getDescItemTypes() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/descItemTypes')
    }

    getCalendarTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/calendarTypes');
    }

    getTemplates(code = null) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/templates', code ? {code} : null);
    }

    getPacketTypes() {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/packets/types');
    }

    getPackets(fundId, text = null, limit = 100) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/packets/' + fundId + '/find/form', null, {'limit': limit, 'text': text});
    }

    findPackets(fundId, state = 'OPEN', prefix = null) {
        const data = {
            prefix,
            state,
        };
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/packets/' + fundId + '/find', null, data);
    }

    deletePackets(fundId, packetIds) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/packets/' + fundId, null, {packetIds});
    }

    setStatePackets(fundId, packetIds, state) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/packets/' + fundId, null, {packetIds, state});
    }

    generatePackets(fundId, prefix, packetTypeId, fromNumber, lenNumber, count, packetIds) {
        const data = {
            prefix, packetTypeId, fromNumber, lenNumber, count, packetIds
        };
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/packets/' + fundId + '/generate', null, data);
    }

    insertPacket(fundId, storageNumber, packetTypeId, invalidPacket) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/packets/' + fundId, {}, {packetTypeId, storageNumber, invalidPacket});
    }

    getFundNodeForm1(versionId, nodeId) {
        const node = findNodeById(_faRootNode, nodeId);
        const data = {
            node: node,
            data: {
                groups:[
                    {
                    name: 'group 1',
                    attrDesc: [
                        { id:1, name: 'Ref. ozn.', multipleValue: false, code: 'STRING', values: [{value: ''}], width: 1},
                        { id:2, name: 'Obsah/regest', multipleValue: false, code: 'TEXT', values: [{value: ''}], width: 4},
                        { id:4, name: 'Typ archiválie', multipleValue: false, code: 'STRING', values: [{value: ''}], width: 1},
                        { id:3, name: 'Ukládací jednotka', multipleValue: false, code: 'STRING', values: [{value: ''}], width: 1},
                        { id:5, name: 'Datace', multipleValue: false, code: 'STRING', values: [{value: ''}], width: 1},
                        ]
                    },
                ]
            }
        };
        return getData(data, 1);
    }

    getNodeParents(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodeParents', {versionId, nodeId});
    }

    getRequestsInQueue() {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/requests/queued');
    }

    deleteRequestFromQueue(id) {
        return getData({}, 100);
    }

    getDigitizationRequests(versionId, state) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/requests/' + versionId, { state });
    }

    arrRequestAddNodes(versionId, reqId, send, description, nodeIds) {
        const data = {
            id: reqId,
            nodeIds,
            description
        };
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/requests/' + versionId + '/digitization/add', { send } , data);
    }

    arrRequestRemoveNodes(versionId, reqId, nodeIds) {
        const data = {
            id: reqId,
            nodeIds,
        };
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/requests/' + versionId + '/digitization/remove', null, data);
    }

    updateArrRequest(versionId, id, data) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/requests/' + versionId + '/' + id, null , data);
    }

    removeArrRequestQueueItem(id) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/requests/' + id);
    }

    getArrRequests(versionId, type, description) {
        // TODO - předat i filtr, až bude na serveru
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/requests/' + versionId, { });
    }

    getArrRequest(versionId, id) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/requests/' + versionId + "/" + id, { detail: true });
    }

    sendArrRequest(versionId, id) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/requests/' + versionId + "/" + id + "/send");
    }

    deleteArrRequest(versionId, id) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/requests/' + versionId + "/" + id + "/" + id);
    }

    getFundTree(versionId, nodeId, expandedIds={}, includeIds=[]) {
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
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fundTree', null, data);
    }

    getFundTreeNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fundTree/nodes', null, {
            versionId,
            nodeIds
        });
    }

    getPartyNameFormTypes() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/partyNameFormTypes');
    }

    getPartyTypes() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/partyTypes');
    }

    getRuleSets() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/getRuleSets');
    }

    createFund(name, ruleSetId, institutionId, internalCode, dateRange) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/funds', {
            name,
            institutionId,
            ruleSetId,
            internalCode,
            dateRange
        });
    }

    updateFund(data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/updateFund', {ruleSetId: data.ruleSetId}, data)
    }

    approveVersion(versionId, dateRange) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/approveVersion', {dateRange, versionId});
    }

    filterNodes(versionId, filter) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/filterNodes/' + versionId, {}, filter)
    }

    getFilteredNodes(versionId, pageIndex, pageSize, descItemTypeIds) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/getFilterNodes/' + versionId, {page: pageIndex, pageSize: pageSize}, descItemTypeIds)
    }

    replaceDataValues(versionId, descItemTypeId, specsIds, searchText, replaceText, nodes) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/replaceDataValues/' + versionId, {descItemTypeId, searchText, replaceText }, {nodes, specIds: specsIds})
    }

    placeDataValues(versionId, descItemTypeId, specsIds, replaceText, replaceSpecId, nodes) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/placeDataValues/' + versionId, {descItemTypeId, newDescItemSpecId: replaceSpecId, text: replaceText }, {nodes, specIds: specsIds})
    }

    deleteDataValues(versionId, descItemTypeId, specsIds, nodes) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/deleteDataValues/' + versionId, {descItemTypeId}, {nodes, specIds: specsIds})
    }

    getFilteredFulltextNodes(versionId, fulltext, luceneQuery=false) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/getFilteredFulltext/' + versionId, {fulltext, luceneQuery})
    }

    getPackages() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/getPackages');
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/deletePackage/' + code);
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.ruleUrl + '/importPackage', {}, 'POST', data);
    }

    reindex() {
        return AjaxUtils.ajaxGet(WebApi.adminUrl + '/reindex');
    }

    getIndexingState() {
        return AjaxUtils.ajaxGet(WebApi.adminUrl + '/reindexStatus');
    }

    getTransformations() {
        return AjaxUtils.ajaxGet(WebApi.importUrl + '/transformations');
    }

    getExportTransformations() {
        return AjaxUtils.ajaxGet(WebApi.importUrl + '/transformations');
    }

    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.importUrl + '/import', {}, 'POST', data);
    }

    arrCoordinatesImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);

        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/descCoordinates', {}, 'POST', formData);
    }

    arrOutputCoordinatesImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);

        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/outputCoordinates', {}, 'POST', formData);
    }

    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/regCoordinates', {}, 'POST', data);
    }

    descItemCsvImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApi.arrangementUrl + '/descItems/' + versionId + '/csv/import', { }, 'POST', formData);
    }

    descOutputItemCsvImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApi.arrangementUrl + '/outputItems/' + versionId + '/csv/import', { }, 'POST', formData);
    }

    getInstitutions() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/institutions');
    }

    /**
     * Hledá všechny unikátní hodnoty atributu pro daný AS
     */
    getDescItemTypeValues(versionId, descItemTypeId, fulltext, descItemSpecIds, max) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/filterUniqueValues/' + versionId, {descItemTypeId, fulltext, max}, descItemSpecIds)
    }

    getVisiblePolicy(nodeId, fundVersionId, includeParents = true) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId + '/' + includeParents);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/policy/types');
    }

    setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree = false) {
        return AjaxUtils.ajaxPut(WebApi.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId, null, {includeSubtree, policyTypeIdsMap});
    }

    getUserDetail() {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/detail');
    }

    setUserSettings(settings) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/detail/settings', null, settings);
    }

    login(username, password) {
        return AjaxUtils.ajaxCallRaw('/login', {}, 'POST', 'username=' + username + '&password=' + password, 'application/x-www-form-urlencoded');
    }

    logout() {
        return AjaxUtils.ajaxCallRaw('/logout', {}, 'POST', '', 'application/x-www-form-urlencoded', true);
    }

    findFunds(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/getFunds', {fulltext, max})
            .then(json => ({funds: json.list, fundCount: json.count}))
    }

    findUser(fulltext, active, disabled, max = DEFAULT_LIST_SIZE, groupId = null) {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '', {search: fulltext, active, disabled, from: 0, count: max, excludedGroupId: groupId})
            .then(json => ({users: json.rows, usersCount: json.count}))
    }

    changeUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApi.userUrl + "/" + userId + '/permission', null, permissions);
    }

    changeGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApi.userUrl + "/group/" + groupId + '/permission', null, permissions);
    }

    findGroup(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/group', {search: fulltext, from: 0, count: max})
            .then(json => ({groups: json.rows, groupsCount: json.count}))
    }

    getUser(userId) {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/' + userId);
    }

    createGroup(name, code, description) {
        const params = {
            name: name,
            code: code,
            description
        };
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group', null, params);
    }

    updateGroup(groupId, name, description) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/group/' + groupId, null, {name, description});
    }

    deleteGroup(groupId) {
        return AjaxUtils.ajaxDelete(WebApi.userUrl + '/group/' + groupId);
    }

    joinGroup(groupIds, userIds) {
        const data = {
            groupIds: groupIds,
            userIds: userIds
        };
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group/join/', null, data);
    }

    leaveGroup(groupId, userId) {
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group/' + groupId + '/leave/' + userId, null, null);
    }

    createUser(username, password, partyId) {
        const params = {
            username: username,
            password: password,
            partyId: partyId
        }
        return AjaxUtils.ajaxPost(WebApi.userUrl, null, params);
    }

    updateUser(id, username, password) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/' + id, null, {username, password});
    }

    changePasswordUser(oldPassword, newPassword) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/password', null, {oldPassword, newPassword});
    }

    changePassword(userId, newPassword) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/' + userId + '/password', null, {newPassword});
    }

    changeActive(userId, active) {
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/' + userId + '/active/' + active);
    }

    getGroup(groupId){
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/group/' + groupId);
    }

    getFundDetail(fundId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/getFund/' + fundId)
            .then(json => {
                return {
                    ...json,
                    versionId: json.versions[0].id,
                    activeVersion: json.versions[0],
                }
            })
    }

    getValidationItems(fundVersionId, fromIndex, toIndex) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validation/' + fundVersionId + '/' + fromIndex + '/'+ toIndex);
    }

    findValidationError(fundVersionId, nodeId, direction) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validation/' + fundVersionId + '/find/' + nodeId + '/'+ direction);
    }

    deleteFund(fundId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/deleteFund/' + fundId);
    }

    getOutputTypes(versionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/types/' + versionId);
    }

    getOutputs(versionId, state) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/' + versionId + (state != null ? '?state=' + state : ''));
    }

    getFundOutputDetail(versionId, outputId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createOutput(versionId, data) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/output/' + versionId, null, data);
    }

    updateOutput(versionId, outputId, data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/update', null, data);
    }

    outputUsageEnd(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/lock');
    }

    fundOutputAddNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/add', null, nodeIds);
    }

    fundOutputRemoveNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/remove', null, nodeIds);
    }

    outputDelete(versionId, outputId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createFundFile(formData) {
        return AjaxUtils.ajaxCallRaw(WebApi.dmsUrl + '/fund/', {}, 'POST', formData);
    }

    findFundFiles(fundId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApi.dmsUrl + '/fund/' + fundId, {'count': count, 'search': searchText});
    }

    updateFundFile(fileId, formData) {
        return AjaxUtils.ajaxCallRaw(WebApi.dmsUrl + '/fund/' + fileId, {}, 'POST', formData);
    }

    deleteArrFile(fileId) {
        return AjaxUtils.ajaxDelete(WebApi.dmsUrl + '/fund/' + fileId, null, null);
    }

    findFundOutputFiles(resultId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApi.dmsUrl + '/output/' + resultId, {'count': count, 'search': searchText});
    }

    getFundOutputFunctions(outputId, getRecommended) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/output/' + outputId, {'recommended': getRecommended});
    }

    outputGenerate(outputId, forced = false) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/generate/' + outputId, {forced});
    }

    outputRevert(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/revert');
    }

    outputClone(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/clone');
    }

    getRegExternalSystems() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/externalSystems');
    }

    findInterpiRecords(criteria) {
        return AjaxUtils.ajaxPost(WebApi.registryUrl + '/interpi', null, criteria);
    }

    importRecord(importVO) {
        return AjaxUtils.ajaxPost(WebApi.registryUrl + '/interpi/import/', null, importVO);
    }

    importRecordUpdate(recordId, importVO) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/interpi/import/'+ recordId, null, importVO);
    }
}

WebApi.baseUrl = '/api';
WebApi.arrangementUrl = WebApi.baseUrl + '/arrangement';
WebApi.registryUrl = WebApi.baseUrl + '/registry';
WebApi.partyUrl = WebApi.baseUrl + '/party';
WebApi.importUrl = WebApi.baseUrl + '/import';
WebApi.exportUrl = WebApi.baseUrl + '/export';
WebApi.actionUrl = WebApi.baseUrl + '/action';
WebApi.kmlUrl = WebApi.baseUrl + '/kml';
WebApi.ruleUrl = WebApi.baseUrl + '/rule';
WebApi.changesUrl = WebApi.arrangementUrl + '/changes';
WebApi.dmsUrl = WebApi.baseUrl + '/dms';
WebApi.userUrl = WebApi.baseUrl + '/user';
WebApi.adminUrl = WebApi.baseUrl + '/admin';
WebApi.validateUrl = WebApi.baseUrl + '/validate';



export default {
    WebApi: new WebApi(),
    WebApiCls: WebApi,
    UrlFactory: UrlFactory,
};
