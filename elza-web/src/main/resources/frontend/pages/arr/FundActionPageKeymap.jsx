import {Utils} from 'components/index.jsx';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    ArrParent: {
        newAction: keyModifier + 'n',       //CREATE_NEW_ACTION
    }
}

export default defaultKeymap;
