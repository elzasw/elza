import { FormErrors } from 'redux-form';

import { getIntl } from 'components/shared/lang/intlInstance';
import { globalMessages } from 'components/shared/lang/messages';
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
        errors.name = getIntl().formatMessage(globalMessages.validationRequired);
    }
    if (props.ruleSet && !values.ruleSetId) {
        errors.ruleSetId = getIntl().formatMessage(globalMessages.validationRequired);
    }
    if ((props.create || props.ruleSet) && !values.ruleSetCode) {
        errors.ruleSetCode = getIntl().formatMessage(globalMessages.validationRequired);
    }
    if ((props.create || props.update) && !values.institutionIdentifier) {
        errors.institutionIdentifier = getIntl().formatMessage(globalMessages.validationRequired);
    }
    if (props.create && (!values.scopes || values.scopes.length === 0)) {
        // Typy redux-form konvenci `_error` neznají, i když ji knihovna vyžaduje.
        (errors as Record<string, unknown>).scopes = {_error: getIntl().formatMessage(globalMessages.validationRequired)};
    }
    if (props.create && !admin && (!values.fundAdmins || values.fundAdmins.length === 0)) {
        errors.fundAdmins = getIntl().formatMessage(globalMessages.validationRequired);
    }

    return errors;
};
