import {Item} from './Item';

export class ItemRecordRef extends Item {
    toSimpleString() {
        if (this.item.accessPoint === undefined || this.item.accessPoint === null) {
            return null;
        } else {
            return this.item.accessPoint.record;
        }
    }
}
