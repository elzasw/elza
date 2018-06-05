import {Item} from "./Item";

export class ItemRecordRef extends Item {

    constructor(item) {
        super(item);
    }

    toSimpleString() {
        return this.item.record.record;
    };

}
