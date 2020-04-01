import {Utils} from 'components/shared';

var keyModifier = Utils.getKeyModifier();

var defaultKeymap = {
    ArrParent: {
        back: keyModifier + 'z', //BACK
        arr: keyModifier + 'a', //GOTO_ARR_PAGE
        dataGrid: keyModifier + 't', //GOTO_DATAGRID_PAGE
        movements: keyModifier + 'm', //GOTO_MOVEMENTS_PAGE
        actions: keyModifier + 'h', //GOTO_ACTIONS_PAGE
        output: keyModifier + 'o', //GOTO_OUTPUT_PAGE
        TOGGLE_READ_MODE: '',
    },
};

export default defaultKeymap;
