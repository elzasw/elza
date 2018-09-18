import {Item} from "./Item";

export class ItemFileRef extends Item {

    constructor(item) {
        super(item);
    }

    toSimpleString() {
        return this.item.name;
    };

}
