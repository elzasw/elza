import {Utils} from 'components/shared';

var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    FundTreeLazy:{
        "MOVE_UP": "up",
        "MOVE_DOWN": "down",
        "MOVE_TO_PARENT_OR_CLOSE": "left",
        "MOVE_TO_CHILD_OR_OPEN": "right"
    }
}

export default defaultKeymap;
