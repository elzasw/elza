import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Party: {
        addParty: keyModifier + 'n',    //ADD_PARTY
        area1: keyModifier + '1',       //FOCUS_AREA_1
        area2: keyModifier + '2',       //FOCUS_AREA_2
    }
}

export default defaultKeymap;
