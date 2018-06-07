import {Item} from "./Item";

export class ItemPartyRef extends Item {

    constructor(item) {
        super(item);
    }

    toSimpleString() {
        return this.item.party.record.record;
    };

}
