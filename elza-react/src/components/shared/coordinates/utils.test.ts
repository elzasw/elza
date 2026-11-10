import { describe, expect, it } from 'vitest';
import { simplifySinglePartGeometry } from './utils';

describe('simplifySinglePartGeometry', () => {
    it('jednodílný MULTILINESTRING převede na LINESTRING', () => {
        expect(simplifySinglePartGeometry('MULTILINESTRING((14.3 50.05,14.31 50.06,14.32 50.07))'))
            .toBe('LINESTRING(14.3 50.05,14.31 50.06,14.32 50.07)');
    });

    it('jednodílný MULTIPOLYGON převede na POLYGON', () => {
        expect(simplifySinglePartGeometry('MULTIPOLYGON(((0 0,1 0,1 1,0 0)))'))
            .toBe('POLYGON((0 0,1 0,1 1,0 0))');
    });

    it('jednodílný MULTIPOINT převede na POINT', () => {
        expect(simplifySinglePartGeometry('MULTIPOINT((14.3 50.05))')).toBe('POINT(14.3 50.05)');
    });

    it('vícedílnou geometrii nechá beze změny', () => {
        const value = 'MULTILINESTRING ((0 0, 1 1), (2 2, 3 3))';
        expect(simplifySinglePartGeometry(value)).toBe(value);
    });

    it('jednoduchou geometrii nechá beze změny', () => {
        const value = 'LINESTRING (0 0, 1 1)';
        expect(simplifySinglePartGeometry(value)).toBe(value);
    });

    it('nečitelnou hodnotu nechá beze změny', () => {
        expect(simplifySinglePartGeometry('neplatné WKT')).toBe('neplatné WKT');
        expect(simplifySinglePartGeometry('')).toBe('');
    });
});
