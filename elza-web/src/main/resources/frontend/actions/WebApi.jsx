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

    findRecord(search = ''){
        return AjaxUtils.ajaxGet('/api/registryManager/findRecord', [{key: 'search', value: search},{key: 'from', value: '0'},{key: 'count', value: '200'}, {key: 'registerTypeIds', value: ''}])
            .then(json=>{
                return json;
            });
    }
    getRecord(recordId){
        return AjaxUtils.ajaxGet('/api/registryManager/getRecord', [{key: 'recordId', value: recordId}])
            .then(json=>{
                return json;
            });
    }

    findParty(search = ''){
        return AjaxUtils.ajaxGet('/api/partyManager/findParty', [{
            'search': search,
            'from': 0,
            'count' : 200,
            'partyTypeId': null,
            'originator': false
        }]).then(json=>{
            return json;
        });
    }

    getParty(partyId){
        return AjaxUtils.ajaxGet('/api/partyManager/getParty', [{key: 'partyId', value: partyId}])
            .then(json=>{
                return json;
            });
    }
}

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

    getNodeForm(nodeId, versionId) {
        var faId = 'x';
        var str = " {" + faId + "_" + versionId + "}";

        var root = {id: 123, name: 'root node' + str, parentId: null};
        var child1 = {id: 1, name: 'child 1' + str, parentId: 123};
        var child11 = {id: 11, name: 'child 11' + str, parentId: 1};
        var child12 = {id: 12, name: 'child 12' + str, parentId: 1};
        var child2 = {id: 2, name: 'child 2' + str, parentId: 123};
        var child3 = {id: 3, name: 'child 3' + str, parentId: 123};
        var child31 = {id: 31, name: 'child 31' + str, parentId: 3};
        var child32 = {id: 32, name: 'child 32' + str, parentId: 3};
        var child33 = {id: 33, name: 'child 33' + str, parentId: 3};
        var child4 = {id: 4, name: 'child 4' + str, parentId: 123};

        var parents, children;
        switch (nodeId) {
            case 1:
                parents = [root];
                children = [child11, child12];
            break;
            case 2:
                parents = [root];
                children = [];
            break;
            case 3:
                parents = [root];
                children = [child31, child32, child33];
            break;
            case 4:
                parents = [root];
                children = [];
            break;
            case 11:
                parents = [child1, root];
                children = [];
            break;
            case 12:
                parents = [child1, root];
                children = [];
            break;
            case 31:
                parents = [child3, root];
                children = [];
            break;
            case 32:
                parents = [child3, root];
                children = [];
            break;
            case 33:
                parents = [child3, root];
                children = [];
            break;
            default:
                parents = [];
                children = [];
        }
        var data = {
            parents: parents,
            children: children,
        }
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

    getFaTree(faId, versionId) {
        var str = " {" + faId + "_" + versionId + "}";

        var root = {id: 123, name: 'root node' + str, parentId: null};
        var child1 = {id: 1, name: 'child 1' + str, parentId: 123};
        var child11 = {id: 11, name: 'child 11' + str, parentId: 1};
        var child12 = {id: 12, name: 'child 12' + str, parentId: 1};
        var child2 = {id: 2, name: 'child 2' + str, parentId: 123};
        var child3 = {id: 3, name: 'child 3' + str, parentId: 123};
        var child31 = {id: 31, name: 'child 31' + str, parentId: 3};
        var child32 = {id: 32, name: 'child 32' + str, parentId: 3};
        var child33 = {id: 33, name: 'child 33' + str, parentId: 3};
        var child4 = {id: 4, name: 'child 4' + str, parentId: 123};

        var data = {
            nodeMap: {
                [root.id]: root,
                [child1.id]: child1,
                [child3.id]: child3,
            },
            nodes: [
                {
                    ...root,
                    children: [
                        {...child1, children: [child11, child12]},
                        {...child2},
                        {...child3, children: [child31, child32, child33]},
                        {...child4},
                    ]
                }
            ]
        }

        return this.getData(data, 1);
    }
}

//module.exports = new WebApiRest();
module.exports = new WebApiFake();