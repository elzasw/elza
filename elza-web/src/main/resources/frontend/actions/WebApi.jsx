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

class WebApiRest {
    constructor() {
    }

    getFaFileTree() {
        return AjaxUtils.ajaxGet('/api/arrangementManager/getFindingAids')
            .then(json=>{
                return json.map(i=>{return {id:i.findingAidId, name:i.name}});
            });
    }

    findRegistry(search = null){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/findRecord', {
            search: search,
            from: 0,
            count: 200,
            registerTypeIds: null
        }).then(json=>{
            return json;
        });
    }

    getRegistry(registryId){
        return AjaxUtils.ajaxGet('/api/registryManagerV2/getRecord', {recordId: registryId})
            .then(json=>{
                return json;
            });
    }



    findParty(search = null){
        return AjaxUtils.ajaxGet('/api/partyManager/findParty', {
            search: search,
            from: 0,
            count : 200,
            partyTypeId: null,
            originator: false
        }).then(json=>{
            return json;
        });
    }

    getParty(partyId){
        return AjaxUtils.ajaxGet('/api/partyManager/getParty', {partyId: partyId})
            .then(json=>{
                return json;
            });
    }

    getPackages() {
        return AjaxUtils.ajaxGet('/api/ruleSetManager/getPackages')
            .then(json=>{
                return json;
            });
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet('/api/ruleSetManager/deletePackage/' + code)
                .then(json=>{
                    return json;
                });
    }

    getPackageExportUrl(code) {
        return '/api/ruleSetManager/exportPackage/' + code;
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw('/api/ruleSetManager/importPackage', {}, "POST", data)
                .then(json=>{
                    return json;
                });
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
var _nodeId = 0;
var _faRootNode = {id: 0, name: 'node', depth: 0, parent: null, children: []}
buildTree(_faRootNode, 1);

class WebApiFake {
    constructor() {
    }

    getData(data, timeout = 1000) {
        return new Promise(function (resolve, reject) {
            setTimeout(function() {
                resolve(data);
            }, timeout);
        });
    }

    findParty(filterText){
        var data = 
            [
                {
                    id: 1, name: 'Kněžna Libuše',
                },{
                    id: 2, name: 'Jan Lucemburský',
                },{
                    id: 3, name: 'Marie Terezie',
                },{
                    id: 4, name: 'Svatý Václav',
                },{
                    id: 5, name: 'Albrecht z Valdštejna',
                },{
                    id: 6, name: 'Kouzelník Žito',
                },{
                    id: 7, name: 'Čachtická paní',
                },{
                    id: 8, name: 'Jan "Sladký" Kozina',
                }
            ]
        var filteredData = [];
        for(var i=0; i<data.length; i++){
            if(data[i].name.indexOf(filterText) > -1){
                filteredData[filteredData.length] = data[i]; 
            }
        }
        return this.getData(filteredData, 1);
    }

    getParty(selectedPartyID){
        var data = {
            "id" : selectedPartyID,
            "name": "Jmeno "
        };
        return this.getData(data, 1);
    }   

    getFaFileTree() {
        var data = 
            [
                {
                    id: 1, 
                    name: 'AP1',
                    versions: [{id: 1, name: 'verze 1'}, {id: 2, name: 'verze 2'}]
                },
                {
                    id: 2,
                    name: 'AP2',
                    versions: [{id: 3, name: 'verze 3'}, {id: 4, name: 'verze 4'}]
                }
            ]
        
        return this.getData(data, 1);
    }

    findRegistry(search = '') {
        var data = {
                recordList: [
                    {
                        id: 1, 
                        record: 'Záznam 1',
                    },
                    {
                        id: 2, 
                        record: 'Záznam 2',
                    },
                    {
                        id: 3, 
                        record: 'Záznam 3',
                    },
                    {
                        id: 4, 
                        record: 'Záznam 4',
                    },
                    {
                        id: 5,
                        record: 'Záznam 5',
                    }
                ],
                count: 152
            }
        
        return this.getData(data, 1);
    }

    getRegistry(idRecord) {
        var data = {
            recordId: idRecord,
            registerType: 'text',
            externalSource: 'text1',
            variantRecordList: [{
                    variantRecordId: 1,
                    regRecord: 1,
                    record: 'Záznam variant 2'
                },
                {
                    variantRecordId: 2,
                    regRecord: 2,
                    record: 'Záznam variant 2'
                }
            ],
            record: 'Záznam s názvem id='+idRecord,
            characteristics: 'Charakteristika záznamu s id='+idRecord,
            comment: 'Komentář záznamu s id='+idRecord,
            local: false,
            externalId: ''
        }
        return this.getData(data, 1);
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

    getNodeFormLevel(nodeId, versionId) {
        return AjaxUtils.ajaxGet('/api/arrangementManager/getLevel', {nodeId: 10, versionId: 3})
            .then(json=>{return json})
    }

    getFaNodeForm(faId, nodeId) {
        var node = findNodeById(_faRootNode, nodeId);
        var data = {
            childNodes: [...node.children],
            node: node,
            attrDesc: {a:1, b:2, c:3}
        };
        return this.getData(data, 1);
    }

    getFaNodeInfo(faId, nodeId) {
        var node = findNodeById(_faRootNode, nodeId);
        var parents = [];
        var n = node.parent;
        while (n !== null) {
            parents.push(n);
            n = n.parent;
        }
        var data = {
            childNodes: [...node.children],
            parentNodes: parents
        };
        return this.getData(data, 1);
    }

    getFaTree(faId, versionId, nodeId, expandedIds={}, includeIds=[]) {
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
}

//AjaxUtils.ajaxGet('/api/arrangementManager/getLevel', {nodeId: 10, versionId: 3})
//            .then(json=>console.log("LEVEL", json));

module.exports = new WebApiRest();
//module.exports = new WebApiFake();
