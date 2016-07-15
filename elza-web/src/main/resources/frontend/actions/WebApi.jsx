import {AjaxUtils} from 'components/index.jsx';

function getData(data, timeout = 1000) {
    return new Promise(function (resolve, reject) {
        setTimeout(function() {
            resolve(data);
        }, timeout);
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
        return WebApi.importUrl + '/fund/' + versionId + '?transformationName=' + encodeURIComponent(transformationName);
    }

    static exportRegCoordinate(objectId) {
        return WebApi.kmlUrl + '/export/regCoordinates/' + objectId;
    }

    static exportArrCoordinate(objectId, versionId) {
        return window.location.origin + WebApi.kmlUrl + '/export/descCoordinates/' + versionId + '/' + objectId;
    }

    static exportArrDescItemCsvExport(objectId, versionId) {
        return window.location.origin + WebApi.arrangementUrl + '/descItems/' + versionId + '/csv/export?descItemObjectId=' + objectId
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
class WebApi{
    constructor() {
    }

    findInFundTree(versionId, nodeId, searchText, type) {
        const data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
        }
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fulltext', null,  data);
    }

    getFundsByVersionIds(versionIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/getVersions', null, {ids: versionIds});
    }

    getNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/nodes', null, {versionId: versionId, ids: nodeIds});
    }

    insertRelation(relation) {
        return AjaxUtils.ajaxPost(WebApi.partyUrl + '/relations', null,  relation)
            .then(json=>{
                return json;
            });
    }

    updateRelation(relation) {
        return AjaxUtils.ajaxPut(WebApi.partyUrl + '/relations/'+relation.relationId, null,  relation)
            .then(json=>{
                return json;
            });
    }

    copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/copyOlderSiblingAttribute/', {versionId, descItemTypeId},  {id: nodeId, version: nodeVersionId});
    }

    deleteRelation(relationId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/relations/'+relationId, {relationId: relationId})
            .then(json=>{
                return json;
            });
    }
   
    findParty(search = null, versionId = null){
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/findParty', {
            search: search,
            from: 0,
            count : 200,
            partyTypeId: null,
            versionId: versionId
        }).then(json=>{
            return json.recordList;
        });
    }

    findPartyForParty(partyId, search = null){
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/findPartyForParty', {
            search: search,
            from: 0,
            count : 200,
            partyId: partyId
        }).then(json=>{
            return json.recordList;
        });
    }


    deleteParty(partyId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/deleteParty', {partyId: partyId})
            .then(json=>{
                return json;
            });
    }

    getParty(partyId){
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/getParty', {partyId: partyId})
            .then(json=>{
                return json;
            });
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet(WebApi.validateUrl + '/unitDate', {value: value || ""});
    }

    insertParty(party) {
        return AjaxUtils.ajaxPost(WebApi.partyUrl + '/insertParty', null,  party)
            .then(json=>{
                return json;
            });
    }

    updateParty(party) {
        return AjaxUtils.ajaxPut(WebApi.partyUrl + '/updateParty/'+party.partyId, null,  party)
            .then(json=>{
                return json;
            });
    }

    aainsertRelation(partyId, relationTypeId, note, sources, from, to, entities) {
        const data = {
            partyId: partyId,
            ParRelationTypeVO : {relationTypeId: relationTypeId},
            note: note,
            sources: sources,
            from: from,
            to: to,
            relationEntities: entities
        }
        return AjaxUtils.ajaxPost(WebApi.partyUrl + '/relations', null,  data)
            .then(json=>{
                return json;
            });
    }

    moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelUnder', null, data)
    }

    moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelBefore', null, data)
    }

    moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/moveLevelAfter', null, data)
    }

    deleteName(nameId) {
        return AjaxUtils.ajaxDelete(WebApi.partyUrl + '/deleteName', {nameId: nameId})
            .then(json=>{
                return json;
            });
    }

    createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + "/" + nodeId + "/" + nodeVersionId + "/" + descItemTypeId + "/create", null,  descItem);
    }
    
    createOutputItem(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/outputItems/' + versionId + "/" + outputDefinitionId + "/" + outputDefinitionVersion + "/" + descItemTypeId + "/create", null,  descItem);
    }

    updateDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + "/" + nodeVersionId + "/update/true", null,  descItem);
    }

    updateOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/outputItems/' + versionId + "/" + outputDefinitionVersion + "/update/true", null,  descItem);
    }
    
    deleteDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/descItems/' + versionId + "/" + nodeVersionId + "/delete", null,  descItem);
    }

    deleteOutputItem(versionId, outputDefinitionVersion, descItem) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/outputItems/' + versionId + "/" + outputDefinitionVersion + "/delete", null,  descItem);
    }

    deleteDescItemType(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/descItems/' + versionId + "/" + nodeId + "/" + nodeVersionId + "/" + descItemTypeId, null, null);
    }
    
    deleteOutputItemType(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/outputItems/' + versionId + "/" + outputDefinitionId + "/" + outputDefinitionVersion + "/" + descItemTypeId, null, null);
    }

    switchOutputCalculating(fundVersionId, outputDefinitionId, itemTypeId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + outputDefinitionId + "/" + fundVersionId + "/" + itemTypeId + "/switch", null, null);
    }

    addNode(node, parentNode, versionId, direction, descItemCopyTypes, scenarioName) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/levels', null, {
            versionId,
            direction,
            staticNodeParent: parentNode,
            staticNode: node,
            descItemCopyTypes,
            scenarioName
        });
    }

    deleteNode(node, parentNode, version) {
        return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/levels', null, {
            versionId: version,
            staticNodeParent: parentNode,
            staticNode: node
        });
    }

    getNodeAddScenarios(node, versionId, direction, withGroups = false) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/scenarios', {withGroups: withGroups}, {
            versionId,
            direction,
            node
        });
    }

    findRegistry(search = null, registryParent = null, registerTypeId = null, versionId = null){
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/findRecord', {
            search: search,
            from: 0,
            count: 200,
            parentRecordId: registryParent,
            registerTypeId: registerTypeId,
            versionId: versionId
        }).then(json=>{
            return json;
        });
    }

    findRecordForRelation(search = null, roleTypeId = null, partyId = null) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/findRecordForRelation',{
            search: search,
            from: 0,
            count: 200,
            roleTypeId: roleTypeId,
            partyId: partyId
        }).then(json=>{
            return json;
        })

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
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/action/' + bulkActionRunId + "/interrupt", null);
    }

    queueBulkAction(versionId, code) {
        return AjaxUtils.ajaxGet(WebApi.actionUrl + '/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId, code, nodeIds) {
        return AjaxUtils.ajaxPost(WebApi.actionUrl + '/queue/' + versionId + '/' + code, null, nodeIds);
    }


    getRegistry(registryId){
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/getRecord', {recordId: registryId})
            .then(json=>{
                return json;
            });
    }

    versionValidate(versionId, showAll = false) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validateVersion/' + versionId + '/' + showAll, null)
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/validateVersionCount/' + versionId, null)
    }

    createRecord(record, characteristics, registerTypeId, parentId, scopeId) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/createRecord', null, {
            "@class": "cz.tacr.elza.controller.vo.RegRecordVO",
            record,
            characteristics,
            local: false,
            scopeId,
            parentRecordId: parentId,
            registerTypeId
        })
        .then(json=> {
            return json;
        });
    }

    getFundPolicy(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/fund/policy/' + fundVersionId, {});
    }

    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/fundScopes', {versionId: versionId})
            .then(json=>{
                return json
            });
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/scopes', null)
            .then(json=> {
                return json
            });
    }

    deleteRegistry(recordId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/deleteRecord', {recordId}, null)
            .then(json=>{
                return json;
            });
    }
    getRecordTypesForAdd(partyTypeId = null){
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/recordTypesForPartyType', {partyTypeId: partyTypeId})
            .then(json=>{
                return json;
            });
    }

    updateRegistry(data) {
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/updateRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    deleteVariantRecord(variantRecordId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/deleteVariantRecord', {variantRecordId}, null)
            .then(json=>{
                return json;
            });
    }

    addRegistryVariant(data){
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/createVariantRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    editRegistryVariant(data){
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/updateVariantRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    deleteRegCoordinates(coordinatesId) {
        return AjaxUtils.ajaxDelete(WebApi.registryUrl + '/deleteRegCoordinates', {coordinatesId}, null)
            .then(json=>{
                return json;
            });
    }

    createRegCoordinates(data){
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/createRegCoordinates', null, data)
            .then(json=>{
                return json;
            });
    }

    updateRegCoordinates(data){
        return AjaxUtils.ajaxPut(WebApi.registryUrl + '/updateRegCoordinates', null, data)
            .then(json=>{
                return json;
            });
    }

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
        }
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
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms')
    }

    getFundNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/form')
                .then(json=>{
                    return json
                });
    }

    deleteFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/delete', null, data)
                .then(json=>{
                    return json
                });
    }

    createFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/create', null, data)
                .then(json=>{
                    return json
                });
    }

    updateFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/update', null, data)
                .then(json=>{
                    return json
                });
    }

    getRulDataTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/dataTypes')
            .then(json=>{
                return json
            });
    }

    getDescItemTypes() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/descItemTypes')
    }

    getCalendarTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/calendarTypes')
                .then(json=>{
                    return json
                });
    }

    getTemplates() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/templates');
    }

    getRegisterTypes(partyTypeId) {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/recordTypes', {partyTypeId: partyTypeId})
                .then(json=>{
                    return json
                });
    }

    getPacketTypes() {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/packets/types')
                .then(json=>{
                    return json
                });
    }

    getPackets(fundId, text = null, limit = 100) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/packets/' + fundId + '/find/form', null, {'limit': limit, 'text': text})
                .then(json=>{
                    return json
                });
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
        const data = {
            packetTypeId: packetTypeId, storageNumber: storageNumber, invalidPacket: invalidPacket
        };
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/packets/' + fundId, {}, data);
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
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/nodeParents', {versionId, nodeId})
            .then(json=>{
                return json
            });
    }

    getFundTree(versionId, nodeId, expandedIds={}, includeIds=[]) {
        const data = {
            versionId,
            nodeId,
            includeIds,
            expandedIds: []
        };
        for (let prop in expandedIds) {
            if (expandedIds[prop]) {
                data.expandedIds.push(prop);
            }
        }
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fundTree', null, data)
            .then(json=>{
                return json
            });
    }

    getFundTreeNodes(versionId, nodeIds) {
        const data = {
            versionId,
            nodeIds
        };
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/fundTree/nodes', null, data);
    }

    getPartyNameFormTypes() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/getPartyNameFormTypes');
    }

    getRecordTypes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/recordTypes');
    }

    getPartyTypes() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/getPartyTypes');
    }

    getRuleSets() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/getRuleSets');
    }



    createFund(name, ruleSetId, institutionId, internalCode, dateRange) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/funds', {
                name: name,
                institutionId: institutionId,
                ruleSetId: ruleSetId,
                internalCode: internalCode,
                dateRange: dateRange
            })
            .then(json=>{
                return json;
            });
    }

    updateFund(data) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/updateFund', {ruleSetId: data.ruleSetId}, data)
    }

    approveVersion(versionId, dateRange) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/approveVersion', {dateRange: dateRange, versionId: versionId})
            .then(json=>{
                return json;
            });
    }

    filterNodes(versionId, filter) {
        // console.log(1111111, filter)
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/filterNodes/' + versionId, {}, {filters: filter})
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
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/getPackages')
            .then(json=>{
                return json;
            });
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/deletePackage/' + code)
                .then(json=>{
                    return json;
                });
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.ruleUrl + '/importPackage', {}, "POST", data)
                .then(json=>{
                    return json;
                });
    }
    
    reindex(){
        return AjaxUtils.ajaxGet(WebApi.adminUrl + '/reindex');
    }    
    
    getIndexingState() {
        return AjaxUtils.ajaxGet(WebApi.adminUrl + '/reindexStatus')
            .then(json=>{
                return json;
            });
    }    
    
    getDefaultScopes() {
        return AjaxUtils.ajaxGet(WebApi.registryUrl + '/defaultScopes')
            .then(json=>{
                return json
            });
    }    
    
    getTransformations() {
        return AjaxUtils.ajaxGet(WebApi.importUrl + '/transformations')
            .then(json=>{
                return json
            });
    }

    getExportTransformations() {
        return AjaxUtils.ajaxGet(WebApi.importUrl + '/transformations')
            .then(json=>{
                return json
            });
    }
    
    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.importUrl + '/import', {}, "POST", data);
    }

    arrCoordinatesImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);

        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/descCoordinates', {}, "POST", formData);
    }

    arrOutputCoordinatesImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);

        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/outputCoordinates', {}, "POST", formData);
    }

    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApi.kmlUrl + '/import/regCoordinates', {}, "POST", data);
    }

    descItemCsvImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApi.arrangementUrl + "/descItems/" + versionId + "/csv/import", { }, "POST", formData);
    }
    
    descOutputItemCsvImport(versionId, outputDefinitionId, outputDefinitionVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('outputDefinitionId', outputDefinitionId);
        formData.append('outputDefinitionVersion', outputDefinitionVersion);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(WebApi.arrangementUrl + "/outputItems/" + versionId + "/csv/import", { }, "POST", formData);
    }
    
    getInstitutions() {
        return AjaxUtils.ajaxGet(WebApi.partyUrl + '/institutions');
    }
    // Hledá všechny unikátní hodnoty atributu pro daný AS
    getDescItemTypeValues(versionId, descItemTypeId, filterText, descItemSpecIds, max) {
        return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/filterUniqueValues/' + versionId, 
            { descItemTypeId, fulltext: filterText, max }, descItemSpecIds)
    }

    getVisiblePolicy(nodeId, fundVersionId, includeParents = true) {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId + '/' + includeParents);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet(WebApi.ruleUrl + '/policy/types');
    }

    setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree = false) {
        const data = {
            includeSubtree,
            policyTypeIdsMap
        }
        return AjaxUtils.ajaxPut(WebApi.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId, null, data);
    }

    getUserDetail() {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/detail');
    }

    login(username, password) {
        return AjaxUtils.ajaxCallRaw('/login', {}, "POST", "username=" + username + "&password=" + password, "application/x-www-form-urlencoded");
    }

    logout() {
        return AjaxUtils.ajaxCallRaw('/logout', {}, "POST", "", "application/x-www-form-urlencoded", true);
    }

    findFunds(fulltext, max = 200) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/getFunds', {fulltext, max})
            .then(json => ({funds: json.list, fundCount: json.count}))
    }

    findUser(fulltext, active, disabled, max = 200) {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '', {search: fulltext, active, disabled, from: 0, count: max})
            .then(json => ({users: json.list, usersCount: json.totalCount}))
    }
    
    findGroup(fulltext, max = 200) {
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/group', {search: fulltext, from: 0, count: max})
            .then(json => ({groups: json.list, groupsCount: json.totalCount}))
    }

    getUser(userId){
        return AjaxUtils.ajaxGet(WebApi.userUrl + '/' + userId);
    }

    createGroup(name, code) {
        var params = {
            name: name,
            code: code
        }
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group', null, params);
    }

    changeGroup(groupId, name, descriptiom) {
        var params = {
            name: name,
            descriptiom: descriptiom
        }
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/group/' + groupId, null, params);
    }

    deleteGroup(groupId) {
        return AjaxUtils.ajaxDelete(WebApi.userUrl + '/group/' + groupId);
    }

    joinGroup(groupIds, userIds) {
        var data = {
            groupIds: groupIds,
            userIds: userIds
        };
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group/join/', null, data);
    }

    leaveGroup(groupId, userId) {
        return AjaxUtils.ajaxPost(WebApi.userUrl + '/group/' + groupId + '/leave/' + userId, null, null);
    }

    createUser(username, password, partyId) {
        var params = {
            username: username,
            password: password,
            partyId: partyId
        }
        return AjaxUtils.ajaxPost(WebApi.userUrl, null, params);
    }

    changePasswordUser(oldPassword, newPassword) {
        var params = {
            oldPassword: oldPassword,
            newPassword: newPassword
        }
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/password', null, params);
    }

    changePassword(userId, newPassword) {
        var params = {
            newPassword: newPassword
        }
        return AjaxUtils.ajaxPut(WebApi.userUrl + '/' + userId + '/password', null, params);
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

    getOutputTypes() {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/types');
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
        return AjaxUtils.ajaxCallRaw(WebApi.dmsUrl + '/fund/', {}, "POST", formData);
    }
    
    findFundFiles(fundId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApi.dmsUrl + '/fund/' + fundId, {'count': count, 'search': searchText});
    }

    updateFundFile(fileId, formData) {
        return AjaxUtils.ajaxCallRaw(WebApi.dmsUrl + '/fund/' + fileId, {}, "POST", formData);
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

    outputGenerate(outputId) {
        return AjaxUtils.ajaxGet(WebApi.arrangementUrl + '/output/generate/' + outputId);
    }

    outputRevert(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/revert');
    }

    outputClone(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApi.arrangementUrl + '/output/' + versionId + '/' + outputId + '/clone');
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
WebApi.dmsUrl = WebApi.baseUrl + '/dms';
WebApi.userUrl = WebApi.baseUrl + '/user';
WebApi.adminUrl = WebApi.baseUrl + '/admin';
WebApi.validateUrl = WebApi.baseUrl + '/validate';



module.exports = {
    WebApi: new WebApi(),
    WebApiCls: WebApi,
    UrlFactory: UrlFactory,
};
