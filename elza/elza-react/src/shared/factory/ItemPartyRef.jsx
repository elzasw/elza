import {Item} from "./Item";

export class ItemPartyRef extends Item {

    toSimpleString() {
        if(this.item.party===undefined||this.item.party===null) {
            return null;
        } else {
            return this.item.party.accessPoint.record;
        }
    };

}
