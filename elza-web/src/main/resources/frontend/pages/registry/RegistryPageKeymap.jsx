import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Registry: {
        addRegistry: keyModifier + 'n',         //ADD_REGISTRY
        registryMove: keyModifier + 'x',        //REGISTRY_MOVE
        registryMoveApply: keyModifier + 'v',   //REGISTRY_MOVE_APPLY
        registryMoveCancel: keyModifier + 'w',  //REGISTRY_MOVE_CANCEL
        area1: keyModifier + '1',               //FOCUS_AREA_1
        area2: keyModifier + '2'                //FOCUS_AREA_2
    }
}
export default defaultKeymap;
