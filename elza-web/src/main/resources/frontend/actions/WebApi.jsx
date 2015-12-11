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

class WebApi {
    constructor() {
    }

    getFaFileTree() {
        return AjaxUtils.ajaxGet('/api/arrangementManager/getFindingAids')
            .then(json=>{
                return json.map(i=>{return {id:i.findingAidId, name:i.name}});
            });
    }
    getFaFileTree2() {
        return new Promise(function (resolve, reject) {
            setTimeout(function() {
                resolve([
                    {id:1, name:'name1'},
                    {id:2, name:'name2'},
                    {id:3, name:'name3'},
                ]);
            }, 1);
        });
    }
}

module.exports = new WebApi();