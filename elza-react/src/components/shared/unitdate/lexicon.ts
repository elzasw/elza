/**
 * Unit-date text forms per language, shared with the server.
 *
 * The file lives in elza-core (`src/main/resources/unitdate/lexicon.json`) and is imported here at
 * build time, so both parsers read one definition; `cases.json` next to it is the corpus both must
 * satisfy (see `corpus.test.ts`). Patterns are regular expressions valid in Java and ECMAScript;
 * `MONTH` in a pattern stands for the alternation of the language's month names.
 */
import lexiconJson from '../../../../../elza-core/src/main/resources/unitdate/lexicon.json';

export interface LexiconCommon {
    intervalDelimiter: string;
    intervalDelimiterAliases: string[];
    spacedIntervalDelimiter: string;
    estimateIntervalDelimiter: string;
    estimateOpen: string[];
    estimateClose: string[];
    /** year AD, one group */
    year: string;
    /** year BC, one group */
    bcYear: string;
    /** ISO full date, groups year, month, day */
    isoDate: string;
    /** time, groups hour, minute, optional second */
    time: string;
}

export interface LexiconLanguage {
    tag: string;
    /** marker of a date BC after the number; no capturing group */
    bc: string;
    /** marker of a century after the number; no capturing group */
    century: string;
    /** groups month (number or name), year */
    yearMonth: string;
    /** groups day, month (number or name), year */
    date: string;
    months: string[];
    monthsShort: string[];
    /** lower-case month name or alias to the month number */
    monthByName: Record<string, number>;
    render: Record<string, string>;
}

export interface Lexicon {
    defaultLanguage: string;
    common: LexiconCommon;
    /** in the order of the file, the default language first */
    languages: LexiconLanguage[];
}

const MONTH_PLACEHOLDER = 'MONTH';

const escapeRegex = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

interface RawLanguage {
    bc: string;
    century: string;
    yearMonth: string;
    date: string;
    months: string[];
    monthsShort?: string[];
    monthAliases?: Record<string, number>;
    render: Record<string, string>;
}

function buildLanguage(tag: string, raw: RawLanguage): LexiconLanguage {
    const monthByName: Record<string, number> = {};
    Object.entries(raw.monthAliases ?? {}).forEach(([name, month]) => (monthByName[name.toLowerCase()] = month));
    raw.months.forEach((name, i) => (monthByName[name.toLowerCase()] = i + 1));
    (raw.monthsShort ?? []).forEach((name, i) => (monthByName[name.toLowerCase()] = i + 1));
    const alternation =
        '(?:' +
        Object.keys(monthByName)
            .sort((a, b) => b.length - a.length || (a < b ? -1 : a > b ? 1 : 0))
            .map(escapeRegex)
            .join('|') +
        ')';
    return {
        tag,
        bc: raw.bc,
        century: raw.century,
        yearMonth: raw.yearMonth.replace(MONTH_PLACEHOLDER, alternation),
        date: raw.date.replace(MONTH_PLACEHOLDER, alternation),
        months: raw.months,
        monthsShort: raw.monthsShort ?? [],
        monthByName,
        render: raw.render,
    };
}

const raw = lexiconJson as unknown as {
    defaultLanguage: string;
    common: LexiconCommon;
    languages: Record<string, RawLanguage>;
};

export const lexicon: Lexicon = {
    defaultLanguage: raw.defaultLanguage,
    common: raw.common,
    languages: Object.entries(raw.languages).map(([tag, language]) => buildLanguage(tag, language)),
};

/**
 * Language of a BCP 47 tag; a tag with a region falls back to its language, an unknown or missing
 * tag to the default language.
 */
export function lexiconLanguage(tag?: string | null): LexiconLanguage {
    const byTag = (t: string) => lexicon.languages.find(language => language.tag === t);
    const found = tag ? byTag(tag) ?? byTag(tag.split('-')[0]) : undefined;
    return found ?? byTag(lexicon.defaultLanguage)!;
}

/**
 * Month number of a month text of the language: a number 1-12 or a month name in any case.
 */
export function monthOf(language: LexiconLanguage, text: string): number {
    const value = text.trim();
    if (/^\d/.test(value)) {
        const month = parseInt(value, 10);
        if (month < 1 || month > 12) {
            throw new Error(`Invalid month: ${text}`);
        }
        return month;
    }
    const month = language.monthByName[value.toLowerCase()];
    if (month === undefined) {
        throw new Error(`Unknown month: ${text}`);
    }
    return month;
}
