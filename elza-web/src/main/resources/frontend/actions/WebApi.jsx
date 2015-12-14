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
        
        return this.getData(data);
    }
}

//module.exports = new WebApiRest();
module.exports = new WebApiFake();