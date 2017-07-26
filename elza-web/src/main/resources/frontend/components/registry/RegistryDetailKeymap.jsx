import {Utils} from 'components/index.jsx';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    RegistryDetail: {
        editRecord: keyModifier + 'e',          //EDIT_RECORD
        goToPartyPerson: keyModifier + 'b',     //GOTO_PARTY
    },
}

export default defaultKeymap;
