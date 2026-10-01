import { describe, it, expect } from 'vitest';

import { checkPasswordPolicy, countCharGroups } from './passwordPolicy';

/**
 * Client-side password policy check; mirrors PasswordPolicyService on the server.
 */
describe('countCharGroups', () => {
    it.each([
        ['a', 1],
        ['abc', 1],
        ['aB', 2],
        ['aB1', 3],
        ['aB1!', 4],
        ['ěŠ', 2],
        ['a b', 2],
        ['1234', 1],
    ])('%s -> %i', (password, expected) => {
        expect(countCharGroups(password)).toBe(expected);
    });
});

describe('checkPasswordPolicy', () => {
    it('accepts anything without a policy', () => {
        expect(checkPasswordPolicy('a', undefined)).toBeNull();
        expect(checkPasswordPolicy('a', {})).toBeNull();
        expect(checkPasswordPolicy('a', { minLength: 0, minCharGroups: -1 })).toBeNull();
    });

    it('checks the minimal length in characters', () => {
        expect(checkPasswordPolicy('abcdefg', { minLength: 8 })).toEqual({ rule: 'minLength', minLength: 8 });
        expect(checkPasswordPolicy('abcdefgh', { minLength: 8 })).toBeNull();
        expect(checkPasswordPolicy('ěšč', { minLength: 3 })).toBeNull();
    });

    it('checks the character groups', () => {
        expect(checkPasswordPolicy('abcDEF', { minCharGroups: 3 })).toEqual({
            rule: 'minCharGroups',
            minCharGroups: 3,
        });
        expect(checkPasswordPolicy('abcDEF1', { minCharGroups: 3 })).toBeNull();
        expect(checkPasswordPolicy('abcDEF1-', { minCharGroups: 9 })).toBeNull();
    });

    it('reports the length first', () => {
        expect(checkPasswordPolicy('aB1', { minLength: 8, minCharGroups: 4 })?.rule).toBe('minLength');
    });
});
