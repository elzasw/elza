import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Registry: {
        addRegistry: keyModifier + 'n',         //ADD_REGISTRY
        area1: keyModifier + '1',               //FOCUS_AREA_1
        area2: keyModifier + '2'                //FOCUS_AREA_2
    }
}
export default defaultKeymap;
