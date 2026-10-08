/**
 * Parser of unit-date texts, the client mirror of the server's `UnitDateConverter`.
 *
 * Parsing is language-independent: every form of every language of the lexicon is accepted
 * whatever the UI language, plus the negative year and the ISO full date. The result is the stored
 * form the server computes (format codes, ISO from/to, estimates), so the corpus
 * `elza-core/src/main/resources/unitdate/cases.json` can assert both parsers agree
 * (`corpus.test.ts`). Rendering of dates is the server's job; the client only shows the text the
 * server sends, and rewrites the user's text for the estimate conversion from the spans of the
 * parsed parts.
 *
 * Stored form: a year BC is the ISO year `1 - year` (1 BC is 0); the format of an interval joins
 * the codes of its ends with "-".
 */
import { lexicon, lexiconLanguage, LexiconLanguage, monthOf } from './lexicon';

export const FORMAT_CENTURY = 'C';
export const FORMAT_YEAR = 'Y';
export const FORMAT_YEAR_MONTH = 'YM';
export const FORMAT_DATE = 'D';
export const FORMAT_DATE_TIME = 'DT';
const FORMAT_DELIMITER = '-';

export interface DateTimeParts {
    year: number;
    month: number;
    day: number;
    hour: number;
    minute: number;
    second: number;
}

/** One date part of the text (one end of an interval, or the whole single date). */
export interface ParsedPart {
    /** format code of the part; a single date-time without seconds is `DT-DT` */
    format: string;
    from: DateTimeParts;
    to: DateTimeParts;
    estimate: boolean;
    century: boolean;
    /** the part as written, brackets included, without surrounding spaces */
    raw: string;
    /** offsets of `raw` in the trimmed input */
    start: number;
    end: number;
}

export interface ParsedUnitdate {
    format: string;
    valueFrom: string;
    valueTo: string;
    valueFromEstimated: boolean;
    valueToEstimated: boolean;
    parts: ParsedPart[];
    /** interval written with "/", both ends estimated */
    estimateInterval: boolean;
}

export class UnitdateParseError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'UnitdateParseError';
    }
}

type Kind = 'YEAR' | 'YEAR_MONTH' | 'DATE' | 'DATE_TIME' | 'ISO_DATE' | 'ISO_DATE_TIME' | 'CENTURY';

interface Form {
    pattern: RegExp;
    kind: Kind;
    bc: boolean;
    language?: LexiconLanguage;
}

type TokenType = 'SINGLE' | 'FROM' | 'TO';

const form = (regex: string, kind: Kind, bc: boolean, language?: LexiconLanguage): Form => ({
    pattern: new RegExp(`^${regex}$`, 'iu'),
    kind,
    bc,
    language,
});

/**
 * The forms in the order they are tried: the forms AD of every language, then the forms BC - the
 * same order as on the server.
 */
function buildForms(): Form[] {
    const { common, languages } = lexicon;
    const forms: Form[] = [];
    forms.push(form(common.year, 'YEAR', false));
    languages.forEach(l => forms.push(form(l.yearMonth, 'YEAR_MONTH', false, l)));
    languages.forEach(l => forms.push(form(`${l.date}\\s+${common.time}`, 'DATE_TIME', false, l)));
    forms.push(form(`${common.isoDate}\\s+${common.time}`, 'ISO_DATE_TIME', false));
    languages.forEach(l => forms.push(form(l.date, 'DATE', false, l)));
    forms.push(form(common.isoDate, 'ISO_DATE', false));
    languages.forEach(l => forms.push(form(`(\\d+)${l.century}`, 'CENTURY', false, l)));
    languages.forEach(l => forms.push(form(`(\\d+)${l.century}${l.bc}`, 'CENTURY', true, l)));
    languages.forEach(l => forms.push(form(`${common.bcYear}${l.bc}`, 'YEAR', true, l)));
    languages.forEach(l => forms.push(form(`${l.yearMonth}${l.bc}`, 'YEAR_MONTH', true, l)));
    languages.forEach(l => forms.push(form(`${l.date}${l.bc}`, 'DATE', true, l)));
    languages.forEach(l => forms.push(form(`${l.date}\\s+${common.time}${l.bc}`, 'DATE_TIME', true, l)));
    return forms;
}

const FORMS = buildForms();

const isLeap = (year: number) => (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;

const daysInMonth = (year: number, month: number) =>
    [31, isLeap(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1];

/** ISO year of a chronological year: 1 BC is the ISO year 0, 2 BC is -1. */
const isoYear = (year: number, beforeChrist: boolean) => (beforeChrist ? -year + 1 : year);

const at = (year: number, month: number, day: number, hour = 0, minute = 0, second = 0): DateTimeParts => ({
    year,
    month,
    day,
    hour,
    minute,
    second,
});

const endOfDay = (year: number, month: number, day: number) => at(year, month, day, 23, 59, 59);

function validDate(year: number, month: number, day: number) {
    if (month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) {
        throw new UnitdateParseError(`Invalid date: ${day}.${month}.${year}`);
    }
}

function time(hour: string, minute: string, second?: string) {
    const h = parseInt(hour, 10);
    const m = parseInt(minute, 10);
    const s = second !== undefined ? parseInt(second, 10) : 0;
    if (h > 23 || m > 59 || s > 59) {
        throw new UnitdateParseError(`Invalid time: ${hour}:${minute}${second !== undefined ? ':' + second : ''}`);
    }
    return { h, m, s };
}

interface Token {
    format: string;
    from: DateTimeParts;
    to: DateTimeParts;
}

function toToken(f: Form, m: RegExpExecArray, negative: boolean, type: TokenType): Token {
    if (f.bc && negative) {
        throw new UnitdateParseError('Double negative not supported');
    }
    const beforeChrist = f.bc || negative;
    const int = (group: number) => parseInt(m[group], 10);
    switch (f.kind) {
        case 'YEAR': {
            const year = isoYear(int(1), beforeChrist);
            return { format: FORMAT_YEAR, from: at(year, 1, 1), to: endOfDay(year, 12, 31) };
        }
        case 'YEAR_MONTH': {
            const year = isoYear(int(2), beforeChrist);
            const month = monthOf(f.language!, m[1]);
            return { format: FORMAT_YEAR_MONTH, from: at(year, month, 1), to: endOfDay(year, month, daysInMonth(year, month)) };
        }
        case 'DATE':
        case 'ISO_DATE': {
            const iso = f.kind === 'ISO_DATE';
            const year = isoYear(int(iso ? 1 : 3), beforeChrist);
            const month = iso ? int(2) : monthOf(f.language!, m[2]);
            const day = int(iso ? 3 : 1);
            validDate(year, month, day);
            return { format: FORMAT_DATE, from: at(year, month, day), to: endOfDay(year, month, day) };
        }
        case 'DATE_TIME':
        case 'ISO_DATE_TIME': {
            const iso = f.kind === 'ISO_DATE_TIME';
            const year = isoYear(int(iso ? 1 : 3), beforeChrist);
            const month = iso ? int(2) : monthOf(f.language!, m[2]);
            const day = int(iso ? 3 : 1);
            validDate(year, month, day);
            const { h, m: minute, s } = time(m[4], m[5], m[6]);
            const withoutSeconds = m[6] === undefined;
            const from = at(year, month, day, h, minute, s);
            return {
                format: withoutSeconds && type === 'SINGLE' ? `${FORMAT_DATE_TIME}${FORMAT_DELIMITER}${FORMAT_DATE_TIME}` : FORMAT_DATE_TIME,
                from,
                to: withoutSeconds ? { ...from, second: 59 } : from,
            };
        }
        case 'CENTURY': {
            let century = int(1);
            if (century === 0 && !beforeChrist) {
                throw new UnitdateParseError('Century 0 does not exist');
            }
            if (beforeChrist) {
                century = -century + 1;
            }
            return { format: FORMAT_CENTURY, from: at((century - 1) * 100 + 1, 1, 1), to: endOfDay(century * 100, 12, 31) };
        }
        default:
            throw new UnitdateParseError(`Unexpected kind ${f.kind}`);
    }
}

/**
 * One date part: an optional estimate in brackets, an optional leading "-" (a year BC), then the
 * first form that matches the whole token.
 */
function parseToken(tokenString: string, type: TokenType, offset: number): ParsedPart {
    const leading = tokenString.length - tokenString.trimStart().length;
    const raw = tokenString.trim();
    if (!raw) {
        throw new UnitdateParseError('Empty date part');
    }
    let token = raw;
    let estimate = false;
    if (token.startsWith('[') && token.endsWith(']')) {
        token = token.slice(1, -1);
        estimate = true;
    }
    let negative = false;
    if (token.startsWith(lexicon.common.intervalDelimiter)) {
        token = token.slice(1);
        negative = true;
    }
    for (const f of FORMS) {
        const m = f.pattern.exec(token);
        if (m) {
            const t = toToken(f, m, negative, type);
            return {
                ...t,
                estimate,
                century: t.format === FORMAT_CENTURY,
                raw,
                start: offset + leading,
                end: offset + leading + raw.length,
            };
        }
    }
    throw new UnitdateParseError(`Unsupported date: ${tokenString}`);
}

const pad = (value: number, length: number) => String(value).padStart(length, '0');

/** ISO local date-time of the stored form; the year has four digits and a sign when negative. */
export function toIso(p: DateTimeParts): string {
    if (p.year > 9999 || p.year < -9999) {
        throw new UnitdateParseError(`Year out of range: ${p.year}`);
    }
    const year = p.year < 0 ? '-' + pad(-p.year, 4) : pad(p.year, 4);
    return `${year}-${pad(p.month, 2)}-${pad(p.day, 2)}T${pad(p.hour, 2)}:${pad(p.minute, 2)}:${pad(p.second, 2)}`;
}

const compareParts = (a: DateTimeParts, b: DateTimeParts) =>
    a.year - b.year || a.month - b.month || a.day - b.day || a.hour - b.hour || a.minute - b.minute || a.second - b.second;

/**
 * Brackets of an estimate to square brackets, aliases of the interval delimiter (en dash) to the
 * delimiter, surrounding white space removed. Every replacement keeps the length, so offsets in the
 * result are offsets in the trimmed input.
 */
function normalize(input: string): string {
    const { common } = lexicon;
    let result = input.trim();
    common.estimateOpen.forEach(open => (result = result.split(open).join('[')));
    common.estimateClose.forEach(close => (result = result.split(close).join(']')));
    common.intervalDelimiterAliases.forEach(alias => (result = result.split(alias).join(common.intervalDelimiter)));
    return result;
}

/**
 * Splits an interval written with "-" and no spaces: a leading "-" is the sign of the first year,
 * "--" the delimiter before a negative second year (`1900-1912`, `-7-2`, `-7--2`).
 *
 * @return the parts with the offset of the second one, null when there is no delimiter
 */
function splitInterval(s: string): { from: string; to: string; toOffset: number } | null {
    const delimiter = lexicon.common.intervalDelimiter;
    const doubled = delimiter + delimiter;
    if (!s.includes(delimiter)) {
        return null;
    }
    let splitAt = doubled;
    if (!s.includes(doubled)) {
        if (!s.startsWith(delimiter)) {
            const parts = s.split(delimiter);
            if (parts.length !== 2) {
                throw new UnitdateParseError(`Invalid interval: ${s}`);
            }
            return { from: parts[0], to: parts[1], toOffset: parts[0].length + 1 };
        }
        splitAt = delimiter;
    }
    const position = s.indexOf(splitAt, 1);
    if (position < 0) {
        throw new UnitdateParseError(`Invalid interval: ${s}`);
    }
    return { from: s.slice(0, position), to: s.slice(position + 1), toOffset: position + 1 };
}

function single(part: ParsedPart): ParsedUnitdate {
    return {
        format: part.format,
        valueFrom: toIso(part.from),
        valueTo: toIso(part.to),
        valueFromEstimated: part.estimate,
        valueToEstimated: part.estimate,
        parts: [part],
        estimateInterval: false,
    };
}

function interval(from: string, fromOffset: number, to: string, toOffset: number, estimateBoth: boolean): ParsedUnitdate {
    const a = parseToken(from, 'FROM', fromOffset);
    const b = parseToken(to, 'TO', toOffset);
    if (compareParts(a.from, b.to) > 0) {
        throw new UnitdateParseError('Invalid interval: from is after to');
    }
    return {
        format: `${a.format}${FORMAT_DELIMITER}${b.format}`,
        valueFrom: toIso(a.from),
        valueTo: toIso(b.to),
        valueFromEstimated: a.estimate || estimateBoth,
        valueToEstimated: b.estimate || estimateBoth,
        parts: [a, b],
        estimateInterval: estimateBoth,
    };
}

/**
 * Parses the text of a unit date.
 *
 * @throws UnitdateParseError when the text is not a unit date
 */
export function parseUnitdate(input: string): ParsedUnitdate {
    const s = normalize(input ?? '');
    const { common } = lexicon;
    if (s.includes(common.estimateIntervalDelimiter)) {
        const parts = s.split(common.estimateIntervalDelimiter);
        if (parts.length !== 2) {
            throw new UnitdateParseError(`Invalid interval: ${s}`);
        }
        return interval(parts[0], 0, parts[1], parts[0].length + 1, true);
    }
    if (s.includes(common.spacedIntervalDelimiter)) {
        const position = s.indexOf(common.spacedIntervalDelimiter);
        const toOffset = position + common.spacedIntervalDelimiter.length;
        return interval(s.slice(0, position), 0, s.slice(toOffset), toOffset, false);
    }
    try {
        return single(parseToken(s, 'SINGLE', 0));
    } catch (e) {
        const split = splitInterval(s);
        if (!split) {
            throw e;
        }
        return interval(split.from, 0, split.to, split.toOffset, false);
    }
}

/**
 * Parses the text, null when it is not a unit date.
 */
export function tryParseUnitdate(input: string): ParsedUnitdate | null {
    try {
        return parseUnitdate(input);
    } catch (e) {
        return null;
    }
}

/**
 * Error message of a text that is not a unit date, undefined for a valid one. An empty text is
 * valid here (an empty value is a deletion, decided by the form).
 */
export function unitdateError(value?: string | null): string | undefined {
    if (!value) {
        return undefined;
    }
    try {
        parseUnitdate(value);
        return undefined;
    } catch (e) {
        return (e as Error).message;
    }
}

/**
 * Whether the text should be offered as an estimate: a century not written as one, alone or as
 * either end of an interval.
 */
export function needsEstimateConfirmation(value: string): boolean {
    const parsed = tryParseUnitdate(value);
    return !!parsed && !parsed.estimateInterval && parsed.parts.some(part => part.century && !part.estimate);
}

const withoutBrackets = (raw: string) => (raw.startsWith('[') && raw.endsWith(']')) || (raw.startsWith('(') && raw.endsWith(')')) ? raw.slice(1, -1) : raw;

/**
 * The text with its centuries marked as estimates: both ends of an interval are centuries and one
 * is not an estimate - the interval is written with "/"; one end is a century and not an estimate -
 * it gets brackets; a single century gets brackets. Other texts are returned as they are. The
 * user's spelling and delimiters are kept.
 */
export function convertToEstimate(value: string): string {
    const parsed = tryParseUnitdate(value);
    if (!parsed || parsed.estimateInterval) {
        return value;
    }
    const text = value.trim();
    if (parsed.parts.length === 2) {
        const [a, b] = parsed.parts;
        const between = text.slice(a.end, b.start);
        if (a.century && b.century && (!a.estimate || !b.estimate)) {
            return `${withoutBrackets(a.raw)}${lexicon.common.estimateIntervalDelimiter}${withoutBrackets(b.raw)}`;
        }
        if (a.century && !a.estimate) {
            return `[${a.raw}]${between}${b.raw}`;
        }
        if (b.century && !b.estimate) {
            return `${a.raw}${between}[${b.raw}]`;
        }
        return value;
    }
    const [part] = parsed.parts;
    if (part.century && !part.estimate) {
        return `[${part.raw}]`;
    }
    return value;
}

export { lexicon, lexiconLanguage };
