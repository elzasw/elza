import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    RegistryDetail: {
        editRecord: keyModifier + 'e',          //EDIT_RECORD
        goToPartyPerson: keyModifier + 'b',     //GOTO_PARTY
    },
}

export default defaultKeymap;
