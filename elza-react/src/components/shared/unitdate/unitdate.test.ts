import { describe, expect, it } from 'vitest';
import cases from '../../../../../elza-core/src/main/resources/unitdate/cases.json';
import { unitdateExamples } from './examples';
import { convertToEstimate, needsEstimateConfirmation, parseUnitdate, unitdateError } from './parse';

interface CorpusCase {
    input: string;
    error?: boolean;
    format?: string;
    from?: string;
    to?: string;
    fromEstimated?: boolean;
    toEstimated?: boolean;
    cs?: string;
    en?: string;
}

const corpus = (cases as { cases: CorpusCase[] }).cases;

/**
 * The contract corpus shared with the server (UnitDateCorpusTest): every input parses to the same
 * stored form here as there, or fails on both sides.
 */
describe('unit-date corpus', () => {
    it.each(corpus.map(c => [c.input, c] as const))('"%s"', (input, c) => {
        if (c.error) {
            expect(() => parseUnitdate(input)).toThrow();
            return;
        }
        const parsed = parseUnitdate(input);
        expect(parsed.format).toBe(c.format);
        expect(parsed.valueFrom).toBe(c.from);
        expect(parsed.valueTo).toBe(c.to);
        expect(parsed.valueFromEstimated).toBe(c.fromEstimated ?? false);
        expect(parsed.valueToEstimated).toBe(c.toEstimated ?? false);
    });

    it.each(
        corpus
            .filter(c => !c.error)
            .flatMap(c => [c.cs, c.en].filter((text): text is string => !!text).map(text => [text, c] as const)),
    )('server text "%s" parses back', (text, c) => {
        const parsed = parseUnitdate(text);
        expect(parsed.format).toBe(c.format);
        expect(parsed.valueFrom).toBe(c.from);
        expect(parsed.valueTo).toBe(c.to);
    });
});

describe('format help examples', () => {
    const examples = Object.entries(unitdateExamples).flatMap(([language, groups]) =>
        Object.values(groups).flatMap((list: string[]) => list.map((example: string) => [language, example] as const)),
    );
    it.each(examples)('%s: "%s" parses', (_language, example) => {
        expect(unitdateError(example)).toBeUndefined();
    });
});

describe('validation', () => {
    it('accepts an empty value and reports an invalid one', () => {
        expect(unitdateError('')).toBeUndefined();
        expect(unitdateError(undefined)).toBeUndefined();
        expect(unitdateError('abc')).toBeDefined();
        expect(unitdateError('1990-1980')).toBeDefined();
    });
});

describe('estimate conversion', () => {
    it('marks a single century', () => {
        expect(needsEstimateConfirmation('20. st.')).toBe(true);
        expect(convertToEstimate('20. st.')).toBe('[20. st.]');
        expect(needsEstimateConfirmation('[20. st.]')).toBe(false);
        expect(needsEstimateConfirmation('20th century')).toBe(true);
        expect(convertToEstimate('20th century')).toBe('[20th century]');
    });

    it('writes an interval of two centuries with a slash', () => {
        expect(convertToEstimate('17st-20st')).toBe('17st/20st');
        expect(convertToEstimate('[17st]-20st')).toBe('17st/20st');
        expect(convertToEstimate('17th century – 20th century')).toBe('17th century/20th century');
        expect(needsEstimateConfirmation('17st/20st')).toBe(false);
    });

    it('brackets the century end of a mixed interval and keeps the delimiter', () => {
        expect(convertToEstimate('17st-1750')).toBe('[17st]-1750');
        expect(convertToEstimate('1650-18st')).toBe('1650-[18st]');
        expect(convertToEstimate('1650 – 18th century')).toBe('1650 – [18th century]');
        expect(convertToEstimate(' 17st-1750 ')).toBe('[17st]-1750');
    });

    it('leaves other texts alone', () => {
        expect(needsEstimateConfirmation('1968')).toBe(false);
        expect(convertToEstimate('1968-1969')).toBe('1968-1969');
        expect(convertToEstimate('abc')).toBe('abc');
    });
});
