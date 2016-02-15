/**
 * Web api pro komunikaci se serverem.
 */

import {AjaxUtils} from 'components';

/*
AjaxUtils.ajaxGet('/api/arrangementManager/getFindingAids')
    .then(json=>{
        console.log(1111, json);
    });
*/

class WebApi{
    constructor() {
    }

    findInFaTree(versionId, nodeId, searchText, type) {
        var data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
        }
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/fulltext', null,  data);
    }

    getFaFileTree() {
        return AjaxUtils.ajaxGet('/api/arrangementManager/getFindingAids')
            .then(json=>{
                return json.map(i=>{return {id:i.findingAidId, name:i.name}});
            });
    }

    getFindingAidsByVersionIds(versionIds) {
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
        return AjaxUtils.ajaxGet('/api/validate/unitDate', {value: value});
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

    getData(data, timeout = 1000) {
        return new Promise(function (resolve, reject) {
            setTimeout(function() {
                resolve(data);
            }, timeout);
        });
    }

    getFaFileTree() {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/getFindingAids');
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

    findRecordForRelation(search = null, roleTypeId = null, partyId = null){
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

    bulkActionRun(versionId, code) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/run/' + versionId + '/' + code, null);
    }

    bulkActionValidate(versionId) {
        return AjaxUtils.ajaxGet('/api/bulkActionManagerV2/validate/' + versionId, null);
    }

    getRegistry(registryId){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/getRecord', {recordId: registryId})
            .then(json=>{
                return json;
            });
    }

    versionValidate(versionId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validateVersion/' + versionId, null)
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/validateVersionCount/' + versionId, null)
    }
    
    insertRegistry(nameMain, characteristics, registerTypeId, parentId, scopeId) {
        var data = {
            record: nameMain,
            characteristics: characteristics,
            local: false,
            scopeId: scopeId,
            parentRecordId: parentId,
            registerTypeId: registerTypeId
            
        }
        return AjaxUtils.ajaxPut('/api/registryManagerV2/createRecord', null,  data)
            .then(json=>{
                return json;
            });
    }



    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/faScopes', {versionId: versionId})
            .then(json=>{
                return json
            });
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet('/api/registryManagerV2/scopes')
            .then(json=> {
                return json
            });
    }

    removeRegistry(registryId) {
        var data = {
            recordId: registryId
        }
        return AjaxUtils.ajaxDelete('/api/registryManagerV2/deleteRecord', data)
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
        return AjaxUtils.ajaxDelete('/api/registryManagerV2/deleteVariantRecord', {variantRecordId: variantRecordId})
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
        return this.getData(data, 1);
    }

    getFaNodeForm(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + nodeId + '/' + versionId + '/formNew')
            .then(json=>{
                return json
            });
    }

    getFaNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + versionId + '/forms', {nodeIds: nodeIds})
    }

    getFaNodeFormNew(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodes/' + nodeId + '/' + versionId + '/formNew')
    }

    getFaNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/form')
                .then(json=>{
                    return json
                });
    }

    deleteFaNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/delete', null, data)
                .then(json=>{
                    return json
                });
    }

    createFaNodeRegister(versionId, nodeId, data) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/registerLinks/' + nodeId + '/' + versionId + '/create', null, data)
                .then(json=>{
                    return json
                });
    }

    updateFaNodeRegister(versionId, nodeId, data) {
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

    getPackets(findingAidId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/packets/' + findingAidId)
                .then(json=>{
                    return json
                });
    }

    insertPacket(findingAidId, storageNumber, packetTypeId, invalidPacket) {

        var data = {packetTypeId: packetTypeId, storageNumber: storageNumber, invalidPacket: invalidPacket};

        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/packets/' + findingAidId, {}, data)
                .then(json=>{
                    return json
                });
    }

    getFaNodeForm1(versionId, nodeId) {
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
        return this.getData(data, 1);
    }

    getNodeParents(versionId, nodeId) {
        return AjaxUtils.ajaxGet('/api/arrangementManagerV2/nodeParents', {versionId, nodeId})
            .then(json=>{
                return json
            });
    }

    getFaTree(versionId, nodeId, expandedIds={}, includeIds=[]) {
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
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/faTree', null, data)
            .then(json=>{
                return json
            });
    }
    getFaTree1(versionId, nodeId, expandedIds={}, includeIds=[]) {
        expandedIds = {...expandedIds};

        var srcNodes;
        if (nodeId == null || typeof nodeId == 'undefined') {
            srcNodes = [_faRootNode];
        } else {
            srcNodes = findNodeById(_faRootNode, nodeId).children;
        }

        var expandedIdsExtension = [];
        var expandedIdsExtOut = [];
        includeIds.forEach(id => {
            var node = findNodeById(_faRootNode, id).parent;
            while (node != null) {
                if (expandedIds[node.id]) { // je rozbalená, nic neděláme
                } else {
                    expandedIds[node.id] = true;
                    expandedIdsExtOut.push(node.id);
                }
                node = node.parent;
            }
        });

        var out = [];
        generateFlatTree(srcNodes, expandedIds, out);
        var data = {
            nodes: out,
            expandedIdsExtension: expandedIdsExtOut,
        }

        return this.getData(data, 1);
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



    createFindingAid(name, ruleSetId, arrangementTypeId) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/findingAids', {name: name, arrangementTypeId: arrangementTypeId, ruleSetId: ruleSetId})
            .then(json=>{
                return json;
            });
    }

    updateFindingAid(data) {
        return AjaxUtils.ajaxPost('/api/arrangementManagerV2/updateFindingAid', null, data)
    }

    approveVersion(versionId, ruleSetId, arrangementTypeId) {
        return AjaxUtils.ajaxPut('/api/arrangementManagerV2/approveVersion', {arrangementTypeId: arrangementTypeId, ruleSetId: ruleSetId, versionId: versionId})
            .then(json=>{
                return json;
            });
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

    getPackageExportUrl(code) {
        return '/api/ruleSetManagerV2/exportPackage/' + code;
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
    
    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw('/api/xmlImportManagerV2/import', {}, "POST", data);
    }    
}

function findNodeById(node, nodeId) {
    if (node.id == nodeId) {
        return node;
    }

    for (var a=0; a<node.children.length; a++) {
        var ret = findNodeById(node.children[a], nodeId);
        if (ret !== null) {
            return ret;
        }
    }

    return null;
}
function generateFlatTree(nodes, expandedIds, out) {
    nodes.each(node => {
        node.hasChildren = node.children && node.children.length > 0;
        out.push(node);
        if (expandedIds[node.id]) {
            generateFlatTree(node.children, expandedIds, out);
        }
    });
}
function buildTree(node, depth) {
    _nodeMap[node.id] = node;

    if (depth > 3) {
        return;
    }
    var len = (depth + depth % 5) * 3;
if (depth == 1) {
len = 40;
}
    for (var a=0; a<len; a++) {
        _nodeId++;
        var child = {id: _nodeId, name: node.name + "_" + _nodeId, depth: depth, children: []};
        child.parent = node;
        node.children.push(child);
        buildTree(child, depth + 1);
    }
}
var _nodeMap = {};
var _nodeId = 1;
var _faRootNode = {id: 0, name: 'Reyl František, ThDr. 1893-1935', depth: 0, parent: null, children: []}
var _ch1 = {id: _nodeId++, name: 'Node', depth: 1, parent: _faRootNode, children: []}

var _ch2 = {id: _nodeId++, name: 'ŽIVOTOPISNÝ MATERIÁL', depth: 1, parent: _faRootNode, children: []}
var _ch21 = {id: _nodeId++, name: 'PÍSEMNOSTI CIZÍCH OSOB', depth: 2, parent: _ch2, children: []}
var _ch211 = {id: _nodeId++, name: 'Úmrtní oznámení', depth: 3, parent: _ch21, children: []}
var _ch212 = {id: _nodeId++, name: 'Diplom čestného členství v Československé straně lidové', depth: 3, parent: _ch21, children: []}
var _ch213 = {id: _nodeId++, name: 'Legitimace účastníka 1. Národní pouti československých katolíkůdo Říma', depth: 3, parent: _ch21, children: []}
var _ch214 = {id: _nodeId++, name: 'Legitimace člena Národního shromáždění', depth: 3, parent: _ch21, children: []}
var _ch215 = {id: _nodeId++, name: 'Legitimace člena Národního shromáždění', depth: 3, parent: _ch21, children: []}
_ch2.children.push(_ch21);
_ch21.children.push(_ch211);
_ch21.children.push(_ch212);
_ch21.children.push(_ch213);
_ch21.children.push(_ch214);
_ch21.children.push(_ch215);

var _ch3 = {id: _nodeId++, name: 'KORESPONDENCE', depth: 1, parent: _faRootNode, children: []}
var _ch31 = {id: _nodeId++, name: 'Osobní', depth: 2, parent: _ch3, children: []}
var _ch32 = {id: _nodeId++, name: 'Blahopřání - přijatá', depth: 2, parent: _ch3, children: []}
var _ch321 = {id: _nodeId++, name: 'Blahopřání k šedesátinám od Župního výboru čsl. strany lidové župy Hradec Králové', depth: 3, parent: _ch32, children: []}
var _ch322 = {id: _nodeId++, name: 'Blahopřání k jmeninám', depth: 3, parent: _ch32, children: []}
var _ch323 = {id: _nodeId++, name: 'Blahopřání k sedmdesátinám od zaměstnanců kapitulního velkostatku Skály (Bischofstein)', depth: 3, parent: _ch32, children: []}
var _ch33 = {id: _nodeId++, name: 'Blahopřání - odeslaná', depth: 2, parent: _ch3, children: []}
var _ch331 = {id: _nodeId++, name: 'Poděkování za blahopřání k sedmdesátinám', depth: 3, parent: _ch33, children: []}
_ch3.children.push(_ch31);
_ch3.children.push(_ch32);
_ch3.children.push(_ch33);
_ch32.children.push(_ch321);
_ch32.children.push(_ch322);
_ch32.children.push(_ch323);
_ch33.children.push(_ch331);

var _ch4 = {id: _nodeId++, name: 'ILUSTRAČNÍ MATERIÁL', depth: 1, parent: _faRootNode, children: []}
var _ch41 = {id: _nodeId++, name: 'Fotografie Františka Reyla', depth: 2, parent: _ch4, children: []}
var _ch411 = {id: _nodeId++, name: 'Portrétní fotografie', depth: 3, parent: _ch41, children: []}
var _ch412 = {id: _nodeId++, name: 'Skupinové fotografie ', depth: 3, parent: _ch41, children: []}
var _ch42 = {id: _nodeId++, name: 'Jiné', depth: 2, parent: _ch4, children: []}
var _ch421 = {id: _nodeId++, name: 'Album fotografií, korespondence a blahopřání', depth: 3, parent: _ch42, children: []}
var _ch422 = {id: _nodeId++, name: 'Album blahopřejných projevů k narozeninám a korespondence', depth: 3, parent: _ch42, children: []}
var _ch423 = {id: _nodeId++, name: 'Album blahopřejných projevů k sedmdesátinám a korespondence', depth: 3, parent: _ch42, children: []}
_ch4.children.push(_ch41);
_ch41.children.push(_ch411);
_ch41.children.push(_ch412);
_ch4.children.push(_ch42);
_ch42.children.push(_ch421);
_ch42.children.push(_ch422);
_ch42.children.push(_ch423);

var _ch5 = {id: _nodeId++, name: 'PÍSEMNOSTI TÝKAJÍCÍ SE RODINNÝCH PŘÍSLUŠNÍKŮ', depth: 1, parent: _faRootNode, children: []}
var _ch51 = {id: _nodeId++, name: 'Poděkování za projev soustrasti (Antonie Bartošková a rodina Reylova-Greifova)', depth: 2, parent: _ch5, children: []}
_ch5.children.push(_ch51);

var _ch6 = {id: _nodeId++, name: 'PÍSEMNOSTI CIZÍCH OSOB', depth: 1, parent: _faRootNode, children: []}
var _ch61 = {id: _nodeId++, name: 'Zbožná upomínka na zesnulého Františka Reyla', depth: 2, parent: _ch6, children: []}
_ch6.children.push(_ch61);

_faRootNode.children.push(_ch2);
_faRootNode.children.push(_ch3);
_faRootNode.children.push(_ch4);
_faRootNode.children.push(_ch5);
_faRootNode.children.push(_ch6);
_faRootNode.children.push(_ch1);
buildTree(_ch1, 2);
_ch1.name = 'Velká testovací data';

//AjaxUtils.ajaxGet('/api/arrangementManager/getLevel', {nodeId: 10, versionId: 3})
//            .then(json=>console.log("LEVEL", json));

//module.exports = new WebApiRestOld2
module.exports = new WebApi();
