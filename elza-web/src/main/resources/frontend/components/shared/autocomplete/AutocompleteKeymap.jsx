import * as Utils from "../../Utils";
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Autocomplete: {
        "MOVE_UP": "up",
        "MOVE_DOWN": "down",
        "MOVE_TO_PARENT_OR_CLOSE": "left",
        "MOVE_TO_CHILD_OR_OPEN": "right",
        "SELECT_ITEM": "enter",
        "OPEN_MENU": "alt+down",
        "CLOSE_MENU": ["escape","alt+up"]
    }
}

export default defaultKeymap;
