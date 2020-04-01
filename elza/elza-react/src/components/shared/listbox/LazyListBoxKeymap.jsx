import * as Utils from '../../Utils';

var keyModifier = Utils.getKeyModifier();

var defaultKeymap = {
    LazyListBox: {
        MOVE_UP: 'up',
        MOVE_DOWN: 'down',
        MOVE_PAGE_UP: 'pageup',
        MOVE_PAGE_DOWN: 'pagedown',
        MOVE_TOP: 'home',
        MOVE_END: 'end',
        ITEM_CHECK: 'space',
        ITEM_SELECT: 'enter',
    },
};

export default defaultKeymap;
