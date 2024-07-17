import { fromDuration } from 'components/validate';
import { DisplayType, JAVA_ATTR_CLASS } from '../../constants';
import { Item } from './Item';
import { CLS } from './factoryConsts';

export class ItemInt extends Item {
    copyItem(withValue = true) {
        // convert HH:mm:ss format to seconds
        let descItem = this.item;

        if (this.refType?.viewDefinition === DisplayType.DURATION) {
            descItem = {
                ...descItem,
                value: fromDuration(descItem.value),
            };
        }

        return {
            [JAVA_ATTR_CLASS]: descItem[CLS],
            descItemSpecId: descItem.descItemSpecId,
            position: descItem.position,
            undefined: descItem.undefined,
            error: descItem.error,
            value: withValue ? descItem.value : null,
        };
    }
}
