import { describe, expect, it } from 'vitest';
import { formatAipSize } from './format';

describe('formatAipSize', () => {
    it('reads the size in the unit it fits', () => {
        expect(formatAipSize(0)).toBe('0 B');
        expect(formatAipSize(512)).toBe('512.0 B');
        expect(formatAipSize(2048)).toBe('2.0 kB');
        expect(formatAipSize(5 * 1024 * 1024)).toBe('5.0 MB');
    });

    // An AIP the digital archive announced and whose package never arrived - or arrived broken -
    // has no size, and a missing size must read as unknown and not as a computed nonsense.
    it('says nothing about a size that is not known', () => {
        expect(formatAipSize(null)).toBe('-');
        expect(formatAipSize(undefined)).toBe('-');
        expect(formatAipSize(-1)).toBe('-');
        expect(formatAipSize(Number.NaN)).toBe('-');
    });
});
