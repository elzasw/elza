import {CLS} from './factoryConsts';
import {JAVA_ATTR_CLASS} from '../../constants';

export class Item {
    constructor(item, refType) {
        this.item = item;
        this.refType = refType;
    }

    toSimpleString() {
        return this.item.value;
    }

    copyItem(withValue = true) {
        return {
            [JAVA_ATTR_CLASS]: this.item[CLS],
            descItemSpecId: this.item.descItemSpecId,
            position: this.item.position,
            undefined: this.item.undefined,
            error: this.item.error,
            value: withValue ? this.item.value : null,
        };
    }
}
