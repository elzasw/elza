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
        return '/api/ruleSetManagerV2/exportPackage/' + code;
    }

    static exportFund(versionId, transformationName) {
        return '/api/xmlExportManagerV2/fund/' + versionId + '?transformationName=' + encodeURIComponent(transformationName);
    }
}

/**
 * Web api pro komunikaci se serverem.
 */
class WebApi{
    constructor() {
    }

    findInFundTree(versionId, nodeId, searchText, type) {
        var data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
        }
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/fulltext', null,  data);
    }

    getFundsByVersionIds(versionIds) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/getVersions', null, {ids: versionIds});
    }

    getNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/nodes', null, {versionId: versionId, ids: nodeIds});
    }

    insertRelation(relation) {
        return AjaxUtils.ajaxPost('/api/partyManagerV2/relations', null,  relation)
            .then(json=>{
                return json;
            });
    }

    updateRelation(relation) {
        return AjaxUtils.ajaxPut('/api/partyManagerV2/relations/'+relation.relationId, null,  relation)
            .then(json=>{
                return json;
            });
    }

    copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/copyOlderSiblingAttribute/', {versionId, descItemTypeId},  {id: nodeId, version: nodeVersionId});
    }

    deleteRelation(relationId) {
        return AjaxUtils.ajaxDelete('/api/partyManagerV2/relations/'+relationId, {relationId: relationId})
            .then(json=>{
                return json;
            });
    }
   
    findParty(search = null, versionId = null){
        return AjaxUtils.ajaxGet('/api/partyManagerV2/findParty', {
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
        return AjaxUtils.ajaxGet('/api/partyManagerV2/findPartyForParty', {
            search: search,
            from: 0,
            count : 200,
            partyId: partyId
        }).then(json=>{
            return json.recordList;
        });
    }


    deleteParty(partyId) {
        return AjaxUtils.ajaxDelete('/api/partyManagerV2/deleteParty', {partyId: partyId})
            .then(json=>{
                return json;
            });
    }

    getParty(partyId){
        return AjaxUtils.ajaxGet('/api/partyManagerV2/getParty', {partyId: partyId})
            .then(json=>{
                return json;
            });
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet('/api/validate/unitDate', {value: value || ""});
    }

    insertParty(party) {
        return AjaxUtils.ajaxPost('/api/partyManagerV2/insertParty', null,  party)
            .then(json=>{
                return json;
            });
    }

    updateParty(party) {
        return AjaxUtils.ajaxPut('/api/partyManagerV2/updateParty/'+party.partyId, null,  party)
            .then(json=>{
                return json;
            });
    }

    aainsertRelation(partyId, relationTypeId, note, sources, from, to, entities) {
        var data = {
            partyId: partyId,
            ParRelationTypeVO : {relationTypeId: relationTypeId},
            note: note,
            sources: sources,
            from: from,
            to: to,
            relationEntities: entities
        }
        return AjaxUtils.ajaxPost('/api/partyManagerV2/relations', null,  data)
            .then(json=>{
                return json;
            });
    }

    moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
        var data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/moveLevelUnder', null, data)
    }

    moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
        var data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/moveLevelBefore', null, data)
    }

    moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
        var data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent
        }
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/moveLevelAfter', null, data)
    }

    deleteName(nameId) {
        return AjaxUtils.ajaxDelete('/api/partyManagerV2/deleteName', {nameId: nameId})
            .then(json=>{
                return json;
            });
    }

    createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/descItems/' + versionId + "/" + nodeId + "/" + nodeVersionId + "/" + descItemTypeId + "/create", null,  descItem);
    }

    updateDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/descItems/' + versionId + "/" + nodeVersionId + "/update/true", null,  descItem);
    }
    
    deleteDescItem(versionId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/descItems/' + versionId + "/" + nodeVersionId + "/delete", null,  descItem);
    }

    deleteDescItemType(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxDelete('/api/arrangementManagerV2/descItems/' + versionId + "/" + nodeId + "/" + nodeVersionId + "/" + descItemTypeId, null, null);
    }

    addNode(node, parentNode, versionId, direction, descItemCopyTypes, scenarioName) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/levels', null, {
            versionId,
            direction,
            staticNodeParent: parentNode,
            staticNode: node,
            descItemCopyTypes,
            scenarioName
        });
    }

    deleteNode(node, parentNode, version) {
        return AjaxUtils.ajaxDelete('/api/arrangementManagerV2/levels', null, {
            versionId: version,
            staticNodeParent: parentNode,
            staticNode: node
        });
    }

    getNodeAddScenarios(node, versionId, direction, withGroups = false) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/scenarios', {withGroups: withGroups}, {
            versionId,
            direction,
            node
        });
    }

    findRegistry(search = null, registryParent = null, registerTypeId = null, versionId = null){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/findRecord', {
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
        return AjaxUtils.ajaxGet('/api/registryManagerV2/findRecordForRelation',{
            search: search,
            from: 0,
            count: 200,
            roleTypeId: roleTypeId,
            partyId: partyId
        }).then(json=>{
            return json;
        })

    }

    getBulkActions(versionId, mandatory = false) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/' + versionId + '/' + mandatory, null);
    }

    getBulkActionsState(versionId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/states/' + versionId, null);
    }

    getBulkActionsList(versionId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/list/' + versionId, null);
    }

    bulkActionValidate(versionId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/validate/' + versionId, null);
    }



    getBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/action/' + bulkActionRunId, null);
    }

    interruptBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/action/' + bulkActionRunId + "/interrupt", null);
    }

    queueBulkAction(versionId, code) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId, code, nodeIds) {
        return AjaxUtils.ajaxPost('/api/bulkActionManagerV2/queue/' + versionId + '/' + code, null, nodeIds);
    }


    getRegistry(registryId){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/getRecord', {recordId: registryId})
            .then(json=>{
                return json;
            });
    }

    versionValidate(versionId, showAll = false) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validateVersion/' + versionId + '/' + showAll, null)
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validateVersionCount/' + versionId, null)
    }

    createRecord(record, characteristics, registerTypeId, parentId, scopeId) {
        return AjaxUtils.ajaxPut('/api/registryManagerV2/createRecord', null, {
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
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/fund/policy/' + fundVersionId, {});
    }

    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/fundScopes', {versionId: versionId})
            .then(json=>{
                return json
            });
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/scopes', null)
            .then(json=> {
                return json
            });
    }

    deleteRegistry(recordId) {
        return AjaxUtils.ajaxDelete('/api/registryManagerV2/deleteRecord', {recordId}, null)
            .then(json=>{
                return json;
            });
    }
    getRecordTypesForAdd(partyTypeId = null){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/recordTypesForPartyType', {partyTypeId: partyTypeId})
            .then(json=>{
                return json;
            });
    }

    updateRegistry(data) {
        return AjaxUtils.ajaxPut('/api/registryManagerV2/updateRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    deleteVariantRecord(variantRecordId) {
        return AjaxUtils.ajaxDelete('/api/registryManagerV2/deleteVariantRecord', {variantRecordId}, null)
            .then(json=>{
                return json;
            });
    }

    addRegistryVariant(data){
        return AjaxUtils.ajaxPut('/api/registryManagerV2/createVariantRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    editRegistryVariant(data){
        return AjaxUtils.ajaxPut('/api/registryManagerV2/updateVariantRecord', null, data)
            .then(json=>{
                return json;
            });
    }

    deleteRegCoordinates(coordinatesId) {
        return AjaxUtils.ajaxDelete('/api/registryManagerV2/deleteRegCoordinates', {coordinatesId}, null)
            .then(json=>{
                return json;
            });
    }

    createRegCoordinates(data){
        return AjaxUtils.ajaxPut('/api/registryManagerV2/createRegCoordinates', null, data)
            .then(json=>{
                return json;
            });
    }

    updateRegCoordinates(data){
        return AjaxUtils.ajaxPut('/api/registryManagerV2/updateRegCoordinates', null, data)
            .then(json=>{
                return json;
            });
    }

    getNodeForm(nodeId, versionId) {
        var node = findNodeById(_faRootNode, nodeId);
        var parents = [];
        var siblings = [...node.parent.children];
        var n = node.parent;
        while (n !== null) {
            parents.push(n);
            n = n.parent;
        }

        var data = {
            parents: parents,
            children: node.children,
            siblings: siblings,
        }
        return getData(data, 1);
    }

    getFundNodeForm(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + versionId + '/forms', {nodeIds: nodeIds})
    }

    getFundNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + versionId + '/forms', {nodeIds: nodeIds})
    }

    getFundNodeFormsWithAround(versionId, nodeId, around) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms')
    }

    getFundNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/form')
                .then(json=>{
                    return json
                });
    }

    deleteFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/delete', null, data)
                .then(json=>{
                    return json
                });
    }

    createFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/create', null, data)
                .then(json=>{
                    return json
                });
    }

    updateFundNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/update', null, data)
                .then(json=>{
                    return json
                });
    }

    getRulDataTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/dataTypes')
            .then(json=>{
                return json
            });
    }

    getDescItemTypes() {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/descItemTypes')
    }

    getCalendarTypes(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/calendarTypes')
                .then(json=>{
                    return json
                });
    }

    getRegisterTypes(partyTypeId) {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/recordTypes', {partyTypeId: partyTypeId})
                .then(json=>{
                    return json
                });
    }

    getPacketTypes() {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/packets/types')
                .then(json=>{
                    return json
                });
    }

    getPackets(fundId, text = null, limit = 100) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/packets/' + fundId + '/find/form', null, {'limit': limit, 'text': text})
                .then(json=>{
                    return json
                });
    }

    findPackets(fundId, state = 'OPEN', prefix = null) {
        var data = {
            prefix,
            state,
        };
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/packets/' + fundId + '/find', null, data);
    }

    deletePackets(fundId, packetIds) {
        return AjaxUtils.ajaxDelete('/api/arrangementManagerV2/packets/' + fundId, null, {packetIds});
    }

    setStatePackets(fundId, packetIds, state) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/packets/' + fundId, null, {packetIds, state});
    }

    generatePackets(fundId, prefix, packetTypeId, fromNumber, lenNumber, count, packetIds) {
        var data = {
            prefix, packetTypeId, fromNumber, lenNumber, count, packetIds
        };
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/packets/' + fundId + '/generate', null, data);
    }

    insertPacket(fundId, storageNumber, packetTypeId, invalidPacket) {
        var data = {
            packetTypeId: packetTypeId, storageNumber: storageNumber, invalidPacket: invalidPacket
        };
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/packets/' + fundId, {}, data);
    }

    getFundNodeForm1(versionId, nodeId) {
        var node = findNodeById(_faRootNode, nodeId);
        var data = {
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
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodeParents', {versionId, nodeId})
            .then(json=>{
                return json
            });
    }

    getFundTree(versionId, nodeId, expandedIds={}, includeIds=[]) {
        var data = {
            versionId,
            nodeId,
            includeIds,
            expandedIds: []
        };
        for (var prop in expandedIds) {
            if (expandedIds[prop]) {
                data.expandedIds.push(prop);
            }
        }
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/fundTree', null, data)
            .then(json=>{
                return json
            });
    }

    getFundTreeNodes(versionId, nodeIds) {
        var data = {
            versionId,
            nodeIds
        };
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/fundTree/nodes', null, data);
    }

    getPartyNameFormTypes() {
        return AjaxUtils.ajaxGet('/api/partyManagerV2/getPartyNameFormTypes');
    }

    getRecordTypes() {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/recordTypes');
    }

    getPartyTypes() {
        return AjaxUtils.ajaxGet('/api/partyManagerV2/getPartyTypes');
    }

    getRuleSets() {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/getRuleSets');
    }



    createFund(name, ruleSetId, institutionId, internalCode, dateRange) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/funds', {
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
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/updateFund', {ruleSetId: data.ruleSetId}, data)
    }

    approveVersion(versionId, dateRange) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/approveVersion', {dateRange: dateRange, versionId: versionId})
            .then(json=>{
                return json;
            });
    }

    filterNodes(versionId, filter) {
        // console.log(1111111, filter)
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/filterNodes/' + versionId, {}, {filters: filter})
    }

    getFilteredNodes(versionId, pageIndex, pageSize, descItemTypeIds) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/getFilterNodes/' + versionId, {page: pageIndex, pageSize: pageSize}, descItemTypeIds)
    }

    replaceDataValues(versionId, descItemTypeId, specsIds, searchText, replaceText, nodes) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/replaceDataValues/' + versionId, {descItemTypeId, searchText, replaceText }, {nodes, specIds: specsIds})
    }

    placeDataValues(versionId, descItemTypeId, specsIds, replaceText, replaceSpecId, nodes) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/placeDataValues/' + versionId, {descItemTypeId, newDescItemSpecId: replaceSpecId, text: replaceText }, {nodes, specIds: specsIds})
    }

    deleteDataValues(versionId, descItemTypeId, specsIds, nodes) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/deleteDataValues/' + versionId, {descItemTypeId}, {nodes, specIds: specsIds})
    }

    getFilteredFulltextNodes(versionId, fulltext, luceneQuery=false) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/getFilteredFulltext/' + versionId, {fulltext, luceneQuery})
    }

    getPackages() {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/getPackages')
            .then(json=>{
                return json;
            });
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/deletePackage/' + code)
                .then(json=>{
                    return json;
                });
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw('/api/ruleSetManagerV2/importPackage', {}, "POST", data)
                .then(json=>{
                    return json;
                });
    }
    
    reindex(){
        return AjaxUtils.ajaxGet('/api/admin/reindex');
    }    
    
    getIndexingState() {
        return AjaxUtils.ajaxGet('/api/admin/reindexStatus')
            .then(json=>{
                return json;
            });
    }    
    
    getDefaultScopes() {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/defaultScopes')
            .then(json=>{
                return json
            });
    }    
    
    getTransformations() {
        return AjaxUtils.ajaxGet('/api/xmlImportManagerV2/transformations')
            .then(json=>{
                return json
            });
    }

    getExportTransformations() {
        return AjaxUtils.ajaxGet('/api/xmlExportManagerV2/transformations')
            .then(json=>{
                return json
            });
    }
    
    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw('/api/xmlImportManagerV2/import', {}, "POST", data);
    }

    arrCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw('/api/kmlManagerV1/import/arrCoordinates', {}, "POST", data);
    }
    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw('/api/kmlManagerV1/import/regCoordinates', {}, "POST", data);
    }

    getInstitutions() {
        return AjaxUtils.ajaxGet('/api/partyManagerV2/institutions');
    }
    // Hledá všechny unikátní hodnoty atributu pro daný AS
    getDescItemTypeValues(versionId, descItemTypeId, filterText, descItemSpecIds, max) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/filterUniqueValues/' + versionId, 
            { descItemTypeId, fulltext: filterText, max }, descItemSpecIds)
    }

    getVisiblePolicy(nodeId, fundVersionId, includeParents = true) {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/policy/' + nodeId + '/' + fundVersionId + '/' + includeParents);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet('/api/ruleSetManagerV2/policy/types');
    }

    setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree = false) {
        var data = {
            includeSubtree,
            policyTypeIdsMap
        }
        return AjaxUtils.ajaxPut('/api/ruleSetManagerV2/policy/' + nodeId + '/' + fundVersionId, null, data);
    }

    getUserDetail() {
        return AjaxUtils.ajaxGet('/api/user/detail');
    }

    login(username, password) {
        return AjaxUtils.ajaxCallRaw('/login', {}, "POST", "username=" + username + "&password=" + password, "application/x-www-form-urlencoded");
    }

    logout() {
        return AjaxUtils.ajaxCallRaw('/logout', {}, "POST", "", "application/x-www-form-urlencoded", true);
    }

    findFunds(fulltext, max=200) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/getFunds', {fulltext, max})
            .then(json => ({funds: json.list, fundCount: json.count}))

        return new Promise(function (resolve, reject) {
            var funds = [
                {id: 1, name: 'Nazev 1', number: '111'},
                {id: 2, name: 'Nazev 2', number: '222'},
                {id: 3, name: 'Nazev 3', number: '333'},
                {id: 4, name: 'Nazev 4', number: '444'},
                {id: 5, name: 'Nazev 5', number: '555'},
            ]

            var ff = []
            funds.forEach(f => {
                if (f.name.toLowerCase().indexOf(fulltext.toLowerCase()) !== -1
                    || f.number.toLowerCase().indexOf(fulltext.toLowerCase()) !== -1) {
                    ff.push(f)
                }
            })

            resolve({
                fundCount: 500,
                funds: ff,
            })
        })
    }

    getFundDetail(fundId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/getFund/' + fundId)
            .then(json => {
                return {
                    ...json,
                    versionId: json.versions[0].id,
                    activeVersion: json.versions[0],
                }
            })
    }

    getValidationItems(fundVersionId, fromIndex, toIndex) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validation/' + fundVersionId + '/' + fromIndex + '/'+ toIndex);
    }

    findValidationError(fundVersionId, nodeId, direction) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validation/' + fundVersionId + '/find/' + nodeId + '/'+ direction);
    }

    deleteFund(fundId) {
        return AjaxUtils.ajaxDelete('/api/arrangementManagerV2/deleteFund/' + fundId);
    }

    getOutputs(versionId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/output/' + versionId);
    }

    getFundOutputDetail(versionId, outputId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/output/' + versionId + '/' + outputId);
    }

    createOutput(versionId, data) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/output/' + versionId, null, data);
    }

    outputUsageEnd(versionId, outputId) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/output/' + versionId + '/' + outputId + '/lock');
    }
    
    fundOutputAddNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/output/' + versionId + '/' + outputId + '/add', null, nodeIds);
    }
    
    fundOutputRemoveNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/output/' + versionId + '/' + outputId + '/remove', null, nodeIds);
    }

    outputDelete(versionId, outputId) {
        return AjaxUtils.ajaxDelete('/api/arrangementManagerV2/output/' + versionId + '/' + outputId);
    }
}

module.exports = {
    WebApi: new WebApi(),
    WebApiCls: WebApi,
    UrlFactory: UrlFactory,
};
