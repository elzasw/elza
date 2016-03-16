import {Utils} from 'components'

import * as types from 'actions/constants/ActionTypes';

var ObjectInfo = class ObjectInfo {
    constructor() {
        this.nodeIds = new Utils.StringSet();
        this.fundIds = new Utils.StringSet();

        this.addNode = this.addNode.bind(this);
        this.addFund = this.addFund.bind(this);
    }

    addNode(node) {
        console.log('addNode', node);
        this.nodeIds.add(node.id);
    }

    addFund(fund) {
        console.log('addFund', fund);
        this.fundIds.add(fund.id);
    }
}
export const ObjectInfo

export function getObjectInfo(objectInfo) {
    return {
        type: types.GET_OBJECT_INFO,
        objectInfo
    }
}
