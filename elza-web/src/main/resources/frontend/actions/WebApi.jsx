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

    getRecord(){
        return AjaxUtils.ajaxGet('/api/registryManager/findRecord', [{key: 'search', value: 'test'},{key: 'from', value: '0'},{key: 'count', value: '200'}, {key: 'registerTypeIds', value: ''}])
            .then(json=>{
                return json.recordList.map(i=>{return {id:i.recordId, name:i.name}});
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

    getRecord() {
        var data = 
            [
                {
                    id: 1, 
                    name: 'Záznam 1',
                },
                {
                    id: 2, 
                    name: 'Záznam 2',
                },
                {
                    id: 3, 
                    name: 'Záznam 3',
                },
                {
                    id: 4, 
                    name: 'Záznam 4',
                },
                {
                    id: 5,
                    name: 'Záznam 5',
                }
            ]
        
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