import React, { Fragment } from 'react';
import { defineMessages, IntlShape } from 'react-intl';
import { joinExamplesHtml, UnitdateExamples, unitdateExamplesFor } from './examples';

/**
 * Values of the `{...}` placeholders of the unit-date format help messages: the examples of the UI
 * language (`examples.ts`), joined with the word "or". Two flavours, for a message rendered as rich
 * text (`FormattedMessage`) and for one rendered as an HTML string (`ignoreTag` +
 * `dangerouslySetInnerHTML`).
 */
export const unitdateHelpMessages = defineMessages({
    or: { id: 'dataType.unitdate.format.or', defaultMessage: 'nebo' },
});

export type UnitdateHelpKey = keyof UnitdateExamples;

function joinExamplesNodes(examples: string[], or: string): React.ReactNode {
    return examples.map((example, i) => (
        <Fragment key={i}>
            {i > 0 && (
                <>
                    {' '}
                    <i>{or}</i>{' '}
                </>
            )}
            {example}
        </Fragment>
    ));
}

/** Placeholder values as React nodes, for `<FormattedMessage values={...}>`. */
export function unitdateHelpValues(intl: IntlShape): Record<UnitdateHelpKey, React.ReactNode> {
    const examples = unitdateExamplesFor(intl.locale);
    const or = intl.formatMessage(unitdateHelpMessages.or);
    const values = {} as Record<UnitdateHelpKey, React.ReactNode>;
    (Object.keys(examples) as UnitdateHelpKey[]).forEach(key => (values[key] = joinExamplesNodes(examples[key], or)));
    return values;
}

/** Placeholder values as HTML strings, for a message formatted with `ignoreTag` and inserted as HTML. */
export function unitdateHelpHtmlValues(intl: IntlShape): Record<UnitdateHelpKey, string> {
    const examples = unitdateExamplesFor(intl.locale);
    const or = intl.formatMessage(unitdateHelpMessages.or);
    const values = {} as Record<UnitdateHelpKey, string>;
    (Object.keys(examples) as UnitdateHelpKey[]).forEach(key => (values[key] = joinExamplesHtml(examples[key], or)));
    return values;
}
