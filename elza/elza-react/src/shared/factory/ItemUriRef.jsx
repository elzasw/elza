import {Item} from './Item';

export class ItemUriRef extends Item {
    copyItem(withValue = true) {
        const result = super.copyItem(withValue);
        result.description = this.item.description;
        result.refTemplateId = this.item.refTemplateId;
        result.nodeId = this.item.nodeId;
        return result;
    }
}
