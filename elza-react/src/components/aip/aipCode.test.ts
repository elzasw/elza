import { describe, expect, it } from 'vitest';
import { aipCodeForms, sameAipCode } from './aipCode';

const UUID = '1659488e-6b5c-4e3b-99aa-7da151f16ec7';

describe('aipCodeForms', () => {
    it('offers a UUID as written, bare and with the prefix', () => {
        expect(aipCodeForms(UUID)).toEqual([UUID, 'uuid-' + UUID]);
        expect(aipCodeForms(' uuid-' + UUID + ' ')).toEqual(['uuid-' + UUID, UUID]);
        expect(aipCodeForms('UUID-' + UUID.toUpperCase())).toEqual(['UUID-' + UUID.toUpperCase(), UUID, 'uuid-' + UUID]);
    });

    it('keeps another identifier as it is', () => {
        expect(aipCodeForms('ABC-1')).toEqual(['ABC-1']);
        expect(aipCodeForms('uuid-not-a-uuid')).toEqual(['uuid-not-a-uuid']);
    });
});

describe('sameAipCode', () => {
    it('matches a UUID in any form', () => {
        expect(sameAipCode(UUID, 'uuid-' + UUID)).toBe(true);
        expect(sameAipCode('UUID-' + UUID.toUpperCase(), UUID)).toBe(true);
        expect(sameAipCode(UUID, 'uuid-00000000-0000-0000-0000-000000000000')).toBe(false);
    });

    it('matches another identifier exactly', () => {
        expect(sameAipCode('ABC-1', 'ABC-1')).toBe(true);
        expect(sameAipCode('ABC-1', 'abc-1')).toBe(false);
    });
});
