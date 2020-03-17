import {CLS} from './factoryConsts';

export class Item {
    constructor(item) {
        this.item = item;
    }

    toSimpleString() {
        return this.item.value;
    }

    copyItem(withValue = true) {
        return {
            '@class': this.item[CLS],
            descItemSpecId: this.item.descItemSpecId,
            position: this.item.position,
            undefined: this.item.undefined,
            error: this.item.error,
            value: withValue ? this.item.value : null,
        };
    }
}
