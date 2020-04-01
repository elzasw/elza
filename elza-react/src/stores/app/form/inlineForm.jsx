/**
 * Obecný store pro inline editaci.
 * !!!Pokud bude třeba vlastní, je nutné zachovat metody z tohoto store!!!
 */
export default function inlineForm(state, action) {
    switch (action.type) {
        case 'redux-form/REPLACE_STATE':
            return action.formState;
        default:
            return state;
    }
}
