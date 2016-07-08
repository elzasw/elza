/**
 * Akce pro inicializaci inline formuláře.
 * @param formName název formuláře
 * @param validate validační metoda
 * @param onSave on save callback v případě validních změněných dat
 * @returns action
 */

export function initForm(formName, validate, onSave) {
    return {
        type: "redux-form/INPLACE_INIT",
        form: formName,
        validate,
        onSave,
    }
}