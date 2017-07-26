import {Utils} from 'components/index.jsx';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    DescItemType: {
        deleteDescItemType: [keyModifier + 'y'],  //DELETE_DESC_ITEM
    },
    DescItem: {
        addDescItem: keyModifier + 'i',     //ADD_DESC_ITEM_PART
        deleteDescItem: keyModifier + 'd',  //DELETE_DESC_ITEM_PART
    },
}

export default defaultKeymap;
