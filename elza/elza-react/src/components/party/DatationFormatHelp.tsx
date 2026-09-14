import { FormattedMessage, defineMessages } from 'react-intl';

/**
 * Nápověda k formátu datace zobrazená v tooltipu u pole datace.
 *
 * Dřív to byla jedna zpráva `dataType.unitdate.format` obsahující celý blok
 * HTML, který se vkládal přes `dangerouslySetInnerHTML`. Pro překladatele to
 * znamenalo editovat značky, a v ICU jsou navíc `<b>`/`<i>` rich-text značky,
 * ne HTML. Text je proto rozepsaný na krátké řádky; příklady dat zůstávají
 * v textu, protože jsou v obou jazycích stejné.
 */
const messages = defineMessages({
    formatHeading: {
        id: 'dataType.unitdate.format.heading',
        defaultMessage: 'Formát datace',
    },
    century: {
        id: 'dataType.unitdate.format.century',
        defaultMessage: 'Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st',
    },
    year: {
        id: 'dataType.unitdate.format.year',
        defaultMessage: 'Rok: 1968',
    },
    month: {
        id: 'dataType.unitdate.format.month',
        defaultMessage: 'Měsíc: 8.1968',
    },
    day: {
        id: 'dataType.unitdate.format.day',
        defaultMessage: 'Den: 21.8.1968',
    },
    time: {
        id: 'dataType.unitdate.format.time',
        defaultMessage: 'Hodiny, minuty, sekundy: 21.8.1968 2:43 <i>nebo</i> 21.8.1968 8:23:31',
    },
    intervalsHeading: {
        id: 'dataType.unitdate.format.intervals.heading',
        defaultMessage: 'Intervaly',
    },
    intervalYears: {
        id: 'dataType.unitdate.format.intervals.years',
        defaultMessage: 'Roky: 1968-1969',
    },
    intervalCombined: {
        id: 'dataType.unitdate.format.intervals.combined',
        defaultMessage: 'Kombinace: 8.1968-1969 <i>nebo</i> 21.8.1968 2:43-27.6.1989',
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
        defaultMessage: 'Např.: [16.8.1977] <i>nebo</i> [1990]-1992',
    },
    estimateSlash: {
        id: 'dataType.unitdate.format.estimate.slash',
        defaultMessage:
            'Při použití znaku "/" pro oddělení intervalu jsou od i do chápány jako odhad:',
    },
    estimateSlashExample: {
        id: 'dataType.unitdate.format.estimate.slashExample',
        defaultMessage: 'Např.: 1985/1990',
    },
});

/** Zvýraznění uvnitř řádku; `<i>` je rich-text značka, ne HTML. */
const emphasis = { i: (chunks: React.ReactNode) => <i>{chunks}</i> };

const Line = ({ message }: { message: (typeof messages)[keyof typeof messages] }) => (
    <>
        <FormattedMessage {...message} values={emphasis} />
        <br />
    </>
);

export function DatationFormatHelp() {
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
            <FormattedMessage {...messages.estimateSlashExample} values={emphasis} />
        </div>
    );
}
