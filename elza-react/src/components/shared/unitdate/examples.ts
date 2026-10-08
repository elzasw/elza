/**
 * Examples of unit-date texts shown in the format help, per UI language.
 *
 * The help messages only carry the labels; the examples come from here, so a help text can never
 * show a form the parser rejects: `unitdate.test.ts` parses every example. Add an example to the
 * corpus (`elza-core/src/main/resources/unitdate/cases.json`) when it shows a new form.
 */
export interface UnitdateExamples {
    century: string[];
    year: string[];
    month: string[];
    day: string[];
    time: string[];
    intervalYears: string[];
    intervalCombined: string[];
    estimateBrackets: string[];
    estimateSlash: string[];
}

export const unitdateExamples: Record<string, UnitdateExamples> = {
    cs: {
        century: ['20. st.', '20.st.', '20st'],
        year: ['1968'],
        month: ['8.1968'],
        day: ['21.8.1968'],
        time: ['21.8.1968 2:43', '21.8.1968 8:23:31'],
        intervalYears: ['1968-1969'],
        intervalCombined: ['8.1968-1969', '21.8.1968 2:43-27.6.1989'],
        estimateBrackets: ['[16.8.1977]', '[1990]-1992'],
        estimateSlash: ['1985/1990'],
    },
    en: {
        century: ['20th century', '1st century BC'],
        year: ['1968', '500 BC'],
        month: ['Aug 1968'],
        day: ['21 Aug 1968'],
        time: ['21 Aug 1968 2:43', '21 Aug 1968 8:23:31'],
        intervalYears: ['1968 – 1969'],
        intervalCombined: ['Aug 1968 – 1969', '21 Aug 1968 2:43 – 27 Jun 1989'],
        estimateBrackets: ['[16 Aug 1977]', '[1990] – 1992'],
        estimateSlash: ['1985/1990'],
    },
};

/**
 * Examples for a UI locale; a locale with a region falls back to its language, an unknown one to
 * Czech.
 */
export function unitdateExamplesFor(locale?: string | null): UnitdateExamples {
    const language = (locale ?? '').split('-')[0];
    return unitdateExamples[language] ?? unitdateExamples.cs;
}

/**
 * Examples joined for a message rendered as HTML: `a <i>or</i> b`.
 *
 * @param or the word between the examples in the UI language
 */
export function joinExamplesHtml(examples: string[], or: string): string {
    return examples.join(` <i>${or}</i> `);
}
