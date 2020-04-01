import {Utils} from 'components/shared';

var keyModifier = Utils.getKeyModifier();

var defaultKeymap = {
    Accordion: {
        prevItem: keyModifier + 'up', //SELECT_PREVIOUS_ITEM
        nextItem: keyModifier + 'down', //SELECT_NEXT_ITEM
        toggleItem: 'shift+enter', //TOGGLE_ITEM
        ACCORDION_MOVE_UP: 'up',
        ACCORDION_MOVE_DOWN: 'down',
        ACCORDION_MOVE_TOP: 'home',
        ACCORDION_MOVE_END: 'end',
    },
    NodePanel: {
        searchItem: keyModifier + 'f', //SEARCH_ITEM
        addDescItemType: [keyModifier + 'p', 'n p'], //ADD_DESC_ITEM
        addNodeAfter: [keyModifier + 'plus', 'n j down'], //ADD_NODE_AFTER
        addNodeBefore: [keyModifier + 'minus', 'n j up'], //ADD_NODE_BEFORE
        addNodeChild: [keyModifier + '*', 'n j right'], //ADD_NODE_CHILD
        addNodeEnd: [keyModifier + '/', 'n j shift+down'], //ADD_NODE_END
    },
};

export default defaultKeymap;
