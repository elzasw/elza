import { describe, it, expect } from 'vitest';
import { fundMovementsSearch, parseFundMovementsSearch, urlFundMovements } from './constants';

describe('urlFundMovements', () => {
    it('bez uzlů vrátí adresu přesunů bez parametrů', () => {
        expect(urlFundMovements(5)).toBe('/fund/5/movements');
        expect(urlFundMovements(5, 12)).toBe('/fund/5/v/12/movements');
    });

    it('uzel levého a pravého stromu přidá jako parametry l a r', () => {
        expect(urlFundMovements(5, undefined, 4)).toBe('/fund/5/movements?l=4');
        expect(urlFundMovements(5, undefined, undefined, 7)).toBe('/fund/5/movements?r=7');
        expect(urlFundMovements(5, 12, 4, 7)).toBe('/fund/5/v/12/movements?l=4&r=7');
    });
});

describe('fundMovementsSearch', () => {
    it('bez uzlů vrátí prázdný řetězec, aby šel porovnat s location.search', () => {
        expect(fundMovementsSearch()).toBe('');
    });
});

describe('parseFundMovementsSearch', () => {
    it('přečte uzly levého a pravého stromu v libovolném pořadí', () => {
        expect(parseFundMovementsSearch('?r=7&l=4')).toEqual({ leftNodeId: 4, rightNodeId: 7 });
        expect(parseFundMovementsSearch('?l=4')).toEqual({ leftNodeId: 4, rightNodeId: undefined });
        expect(parseFundMovementsSearch('')).toEqual({ leftNodeId: undefined, rightNodeId: undefined });
    });

    it('neplatné id ignoruje', () => {
        expect(parseFundMovementsSearch('?l=&r=abc')).toEqual({ leftNodeId: undefined, rightNodeId: undefined });
        expect(parseFundMovementsSearch('?l=0&r=-3')).toEqual({ leftNodeId: undefined, rightNodeId: undefined });
        expect(parseFundMovementsSearch('?l=4.5&r=4;r=7')).toEqual({ leftNodeId: undefined, rightNodeId: undefined });
    });
});
