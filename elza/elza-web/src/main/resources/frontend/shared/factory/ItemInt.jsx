import {Item} from "./Item";
import {normalizeInt, fromDuration, toDuration, normalizeDuration} from '../../components/validate.jsx'

export class ItemInt extends Item {

    constructor(item) {
        super(item);
    }

    copyItem(withValue = true) {
        let result = super.copyItem(withValue);
        if(typeof result.value == "string") {
            // probably duration and not direct int value 
            // -> have to convert it to int
            result.value = parseInt(fromDuration(result.value), 10);
        }
        return result;
    }
}
