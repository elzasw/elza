import { FormErrors } from 'redux-form';

import i18n from 'components/i18n';
import { IFundFormData } from '../../types';

export type FundFormValidationProps = {
    create?: boolean;
    update?: boolean;
    ruleSet?: unknown;
    userDetail: {isAdmin: () => boolean};
};

/**
 * Kontrola vyplnění formuláře archivního souboru.
 *
 * Chyba pole se seznamem hodnot (FieldArray) musí být pod klíčem `_error`, jinak ji
 * redux-form k poli nepřiřadí a uživateli se nezobrazí; ostatní pole nesou chybu přímo.
 */
export const validateFundForm = (
    values: Partial<IFundFormData>,
    props: FundFormValidationProps,
): FormErrors<IFundFormData> => {
    const admin = props.userDetail.isAdmin();

    const errors: FormErrors<IFundFormData> = {};

    if ((props.create || props.update) && !values.name) {
        errors.name = i18n('global.validation.required');
    }
    if (props.ruleSet && !values.ruleSetId) {
        errors.ruleSetId = i18n('global.validation.required');
    }
    if ((props.create || props.ruleSet) && !values.ruleSetCode) {
        errors.ruleSetCode = i18n('global.validation.required');
    }
    if ((props.create || props.update) && !values.institutionIdentifier) {
        errors.institutionIdentifier = i18n('global.validation.required');
    }
    if (props.create && (!values.scopes || values.scopes.length === 0)) {
        // Typy redux-form konvenci `_error` neznají, i když ji knihovna vyžaduje.
        (errors as Record<string, unknown>).scopes = {_error: i18n('global.validation.required')};
    }
    if (props.create && !admin && (!values.fundAdmins || values.fundAdmins.length === 0)) {
        errors.fundAdmins = i18n('global.validation.required');
    }

    return errors;
};
