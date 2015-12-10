/**
 * Web api pro komunikaci se serverem.
 */

import {AjaxUtils} from 'components';

console.log(AjaxUtils);

AjaxUtils.ajaxGet('/api/arrangementManager/getFindingAids')
    .then(json=>{
console.log(json);
    });

class WebApi {
    constructor() {
    }

    getFaFileTree() {
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