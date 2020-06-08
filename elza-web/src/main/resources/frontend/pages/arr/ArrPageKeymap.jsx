import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    ArrParent: {
        area1: keyModifier + '1',           //FOCUS_AREA_1
        area2: [keyModifier + '2',"esc"],   //FOCUS_AREA_2
        area3: keyModifier + '3',           //FOCUS_AREA_3
    }
}

export default defaultKeymap;
