import {Item} from './Item';

export class ItemUnitdate extends Item {

    copyItem(withValue = true) {
        const result = super.copyItem(withValue);
        result.calendarTypeId = this.item.calendarTypeId;
        return result;
    };

}
