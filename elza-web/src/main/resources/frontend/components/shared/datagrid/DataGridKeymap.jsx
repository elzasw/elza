import {Utils} from 'components/index.jsx';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    DataGrid:{
        "MOVE_UP":"up",
        "MOVE_DOWN":"down",
        "MOVE_LEFT":"left",
        "MOVE_RIGHT":"right",
        "ITEM_EDIT":["enter","f2"],
        "ITEM_ROW_CHECK":"space",
        "ITEM_DELETE":"del",
    }
}

export default defaultKeymap;
