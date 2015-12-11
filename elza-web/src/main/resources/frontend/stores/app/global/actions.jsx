import {Utils} from 'components'

export const GLOBAL_GET_OBJECT_INFO = 'GLOBAL_GET_OBJECT_INFO'

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

function getObjectInfo(objectInfo) {
    return {
        type: GET_OBJECT_INFO,
        objectInfo
    }
}

export const globalActions = {
    getObjectInfo: getObjectInfo,
    ObjectInfo: ObjectInfo
}
