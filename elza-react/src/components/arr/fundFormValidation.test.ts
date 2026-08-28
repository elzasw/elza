import { describe, it, expect } from 'vitest';

import { validateFundForm } from './fundFormValidation';

/**
 * Chyba pole se seznamem hodnot (FieldArray) se k poli dostane jen pod klíčem `_error`;
 * prostý řetězec redux-form zahodí a uživatel nedostane žádné upozornění.
 */

// Legacy i18n() nemá v testech načtené texty a vrací '[klíč]'.
const REQUIRED = '[global.validation.required]';

const admin = {userDetail: {isAdmin: () => true}};
const nonAdmin = {userDetail: {isAdmin: () => false}};

const filledFund = {
    name: '555',
    internalCode: '22',
    institutionIdentifier: '1',
    ruleSetCode: 'ZP2015',
    scopes: ['lc-test'],
};

describe('validateFundForm', () => {
    it('hlásí chybějící oblast entit ve tvaru, který redux-form u FieldArray přiřadí k poli', () => {
        const errors = validateFundForm({...filledFund, scopes: []}, {create: true, ...admin});

        expect(errors.scopes).toEqual({_error: REQUIRED});
    });

    it('chybí-li oblast entit úplně, chová se stejně', () => {
        const errors = validateFundForm({...filledFund, scopes: undefined}, {create: true, ...admin});

        expect(errors.scopes).toEqual({_error: REQUIRED});
    });

    it('vyplněný formulář projde bez chyb', () => {
        const errors = validateFundForm(filledFund, {create: true, ...admin});

        expect(errors).toEqual({});
    });

    it('oblast entit se kontroluje jen při zakládání', () => {
        const errors = validateFundForm({...filledFund, scopes: []}, {update: true, ...admin});

        expect(errors.scopes).toBeUndefined();
    });

    it('nevyplněná pole se hlásí prostým textem', () => {
        const errors = validateFundForm({...filledFund, name: '', institutionIdentifier: ''},
            {create: true, ...admin});

        expect(errors.name).toBe(REQUIRED);
        expect(errors.institutionIdentifier).toBe(REQUIRED);
    });

    it('správce musí vyplnit ten, kdo není administrátor', () => {
        const errors = validateFundForm(filledFund, {create: true, ...nonAdmin});

        expect(errors.fundAdmins).toBe(REQUIRED);
    });
});
