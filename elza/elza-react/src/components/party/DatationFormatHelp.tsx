import React from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { unitdateHelpValues } from 'components/shared/unitdate/helpValues';

/**
 * Nápověda k formátu datace zobrazená v tooltipu u pole datace.
 *
 * Text je rozepsaný na krátké řádky; `<i>` je rich-text značka, ne HTML. Příklady dat nejsou v
 * textu: dosazují se z `components/shared/unitdate/examples` podle jazyka rozhraní, takže
 * nápověda nikdy neukáže tvar, který parser nepřijme (test `unitdate.test.ts`).
 */
const messages = defineMessages({
    formatHeading: {
        id: 'dataType.unitdate.format.heading',
        defaultMessage: 'Formát datace',
    },
    century: {
        id: 'dataType.unitdate.format.century',
        defaultMessage: 'Století: {century}',
    },
    year: {
        id: 'dataType.unitdate.format.year',
        defaultMessage: 'Rok: {year}',
    },
    month: {
        id: 'dataType.unitdate.format.month',
        defaultMessage: 'Měsíc: {month}',
    },
    day: {
        id: 'dataType.unitdate.format.day',
        defaultMessage: 'Den: {day}',
    },
    time: {
        id: 'dataType.unitdate.format.time',
        defaultMessage: 'Hodiny, minuty, sekundy: {time}',
    },
    intervalsHeading: {
        id: 'dataType.unitdate.format.intervals.heading',
        defaultMessage: 'Intervaly',
    },
    intervalYears: {
        id: 'dataType.unitdate.format.intervals.years',
        defaultMessage: 'Roky: {intervalYears}',
    },
    intervalCombined: {
        id: 'dataType.unitdate.format.intervals.combined',
        defaultMessage: 'Kombinace: {intervalCombined}',
    },
    estimateHeading: {
        id: 'dataType.unitdate.format.estimate.heading',
        defaultMessage: 'Odhad',
    },
    estimateBrackets: {
        id: 'dataType.unitdate.format.estimate.brackets',
        defaultMessage: 'Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek:',
    },
    estimateBracketsExample: {
        id: 'dataType.unitdate.format.estimate.bracketsExample',
        defaultMessage: 'Např.: {estimateBrackets}',
    },
    estimateSlash: {
        id: 'dataType.unitdate.format.estimate.slash',
        defaultMessage:
            'Při použití znaku "/" pro oddělení intervalu jsou od i do chápány jako odhad:',
    },
    estimateSlashExample: {
        id: 'dataType.unitdate.format.estimate.slashExample',
        defaultMessage: 'Např.: {estimateSlash}',
    },
});

/** Zvýraznění uvnitř řádku; `<i>` je rich-text značka, ne HTML. */
const emphasis = { i: (chunks: React.ReactNode) => <i>{chunks}</i> };

export function DatationFormatHelp() {
    const intl = useIntl();
    const values = { ...emphasis, ...unitdateHelpValues(intl) };

    const Line = ({ message }: { message: (typeof messages)[keyof typeof messages] }) => (
        <>
            <FormattedMessage {...message} values={values} />
            <br />
        </>
    );

    return (
        <div>
            <b><FormattedMessage {...messages.formatHeading} /></b>
            <br />
            <Line message={messages.century} />
            <Line message={messages.year} />
            <Line message={messages.month} />
            <Line message={messages.day} />
            <Line message={messages.time} />
            <b><FormattedMessage {...messages.intervalsHeading} /></b>
            <br />
            <Line message={messages.intervalYears} />
            <Line message={messages.intervalCombined} />
            <b><FormattedMessage {...messages.estimateHeading} /></b>
            <br />
            <Line message={messages.estimateBrackets} />
            <Line message={messages.estimateBracketsExample} />
            <Line message={messages.estimateSlash} />
            <FormattedMessage {...messages.estimateSlashExample} values={values} />
        </div>
    );
}
