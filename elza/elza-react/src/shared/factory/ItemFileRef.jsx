import {Item} from './Item';

export class ItemFileRef extends Item {

    toSimpleString() {
        return this.item.name;
    };

}
