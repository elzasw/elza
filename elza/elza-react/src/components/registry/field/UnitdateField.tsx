import React, {forwardRef, ForwardRefExoticComponent} from 'react';
import {FormInput} from "../../index";
import { defineMessages } from 'react-intl';
import { getIntl } from 'components/shared/lang/intlInstance';
import {
    convertToEstimate as convertParsedToEstimate,
    needsEstimateConfirmation,
    unitdateError,
} from 'components/shared/unitdate/parse';

// Id jsou převzatá z legacy katalogu beze změny. Hlášky vznikají mimo render
// (validace a potvrzovací dialog), proto řetězec.
const messages = defineMessages({
    invalid: { id: 'global.validation.datation.invalid', defaultMessage: 'Vstupní řetězec není validní.' },
    convertToEstimateMessage: {
        id: 'field.unitdate.convertToEstimate.message',
        defaultMessage:
            'Byla vložena datace velkého rozsahu, pravděpodobně se jedná o odhad. Má se hodnota označit jako odhad?',
    },
    convertToEstimateTitle: {
        id: 'field.unitdate.convertToEstimate.title',
        defaultMessage: 'Potvrzení datace',
    },
});
import { showYesNoDialog, YesNoDialogResult } from 'components/shared/dialog';
import ItemTooltipWrapper from 'components/arr/nodeForm/ItemTooltipWrapper';

/**
 * Validation of a unit-date text by the shared parser (`components/shared/unitdate/parse`, the
 * mirror of the server's `UnitDateConverter`). The message is a generic one: the parser's own
 * messages name the failing form in English and are meant for developers.
 *
 * @param value text of the unit date; an empty value is valid (the form turns it into a deletion)
 */
export function validateUnitDate(value?: string): {valid: boolean, message?: string} {
    const isValid = !unitdateError(value);
    return {
        valid: isValid,
        message: isValid ? null : getIntl().formatMessage(messages.invalid),
    };
}

/**
 * The text with its centuries marked as estimates; other texts are returned as they are.
 */
export const convertToEstimate = (value: string) => convertParsedToEstimate(value);

/**
* Shows a confirmation dialog, when the value meets the right criteria (century not formatted as estimate).
* If confirmed, converts the value to an estimate format.
* If canceled, returns undefined.
*/
export const convertToEstimateWithConfirmation = async (value: string, dispatch: any) => {
    const getResult = async () => await dispatch(showYesNoDialog(getIntl().formatMessage(messages.convertToEstimateMessage), getIntl().formatMessage(messages.convertToEstimateTitle)));

    if (needsEstimateConfirmation(value)) {
        const result = await getResult();
        if(result === YesNoDialogResult.CANCEL){return undefined;}
        if(result === YesNoDialogResult.YES){
            return convertToEstimate(value);
        }
    }
    return value;
}

type Props = {
    name: string;
};

const UnitdateField: ForwardRefExoticComponent<Props> = forwardRef(({name, ...rest}, ref) => {
    return <ItemTooltipWrapper holdOnHover={false} showDelay={1000} tooltipTitle="dataType.unitdate.format" style={{width: '100%'}}>
        <FormInput {...rest} ref={ref}/>
    </ItemTooltipWrapper>
});

export default UnitdateField;
