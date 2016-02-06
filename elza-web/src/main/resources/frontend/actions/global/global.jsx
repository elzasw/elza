import {Utils} from 'components'

import * as types from 'actions/constants/ActionTypes';

var ObjectInfo = class ObjectInfo {
    constructor() {
        this.nodeIds = new Utils.StringSet();
        this.faIds = new Utils.StringSet();

        this.addNode = this.addNode.bind(this);
        this.addFa = this.addFa.bind(this);
    }

    addNode(node) {
        console.log('addNode', node);
        this.nodeIds.add(node.id);
    }

    addFa(fa) {
        console.log('addFa', fa);
        this.faIds.add(fa.id);
    }
}
export const ObjectInfo

export function getObjectInfo(objectInfo) {
    return {
        type: types.GET_OBJECT_INFO,
        objectInfo
    }
}
