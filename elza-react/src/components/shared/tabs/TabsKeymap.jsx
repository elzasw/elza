import * as Utils from "../../Utils";
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Tabs: {
        prevTab: keyModifier + 'left',      //SELECT_PREVIOUS_TAB
        nextTab: keyModifier + 'right',     //SELECT_NEXT_TAB
    }
}

export default defaultKeymap;
