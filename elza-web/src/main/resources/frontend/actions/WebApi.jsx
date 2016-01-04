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

    findRecord(search = null){
        return AjaxUtils.ajaxGet('/api/registryManager/findRecord', {
            search: search,
            from: 0,
            count: 200,
            registerTypeIds: null
        }).then(json=>{
            return json;
        });
    }

    getRecord(recordId){
        return AjaxUtils.ajaxGet('/api/registryManager/getRecord', {recordId: recordId})
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

    findRecord(search = '') {
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

    getRecord(idRecord) {
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
        var n = node.parent;
        while (n !== null) {
            parents.push(n);
            n = n.parent;
        }

        var data = {
            node: node,
            parents: parents,
            children: node.children,
        }
        return this.getData(data, 1);
    }

    getFaTree(faId, versionId, nodeId, expandedIds={}, includeIds={}) {
        var srcNodes;
        if (nodeId == null || typeof nodeId == 'undefined') {
            srcNodes = [_faRootNode];
        } else {
            srcNodes = findNodeById(_faRootNode, nodeId).children;
        }

        var out = [];
        generateFlatTree(srcNodes, expandedIds, out);
        var data = {
            nodes: out
        }

        return this.getData(data, 1);
    }
}

module.exports = new WebApiRest();
//module.exports = new WebApiFake();
