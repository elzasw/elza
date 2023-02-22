import React, {forwardRef, ForwardRefExoticComponent} from 'react';
import moment from "moment";
import {Datace} from "../../shared/datace/datace-types";
import {parse} from "components/shared/datace/datace";
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import {FormInput} from "../../index";
import {i18n} from "../../shared";
import { showYesNoDialog, YesNoDialogResult } from 'components/shared/dialog';

const validateUnitDateInput = (input: Datace) => {
    const validationInput = {...input};
    if (input.from) {
        if (input.to) {
            validateUnitDateInput(input.from);
            validateUnitDateInput(input.to);
            if(input.from.c){
                input.from.y = (input.from.c * 100) - 99;
            }
            if(input.to.c){
                input.to.y = input.to.c * 100;
                input.to.M = 11;
                input.to.d = 31;
                input.to.h = 23;
                input.to.m = 59;
                input.to.s = 59;
            }
            if (input.from.bc && input.from.y) {
                input.from.y *= -1;
            }
            if (input.to.bc && input.to.y) {
                input.to.y *= -1;
            }
            const from = moment(input.from).valueOf();
            const to = moment(input.to).valueOf();
            if (from > to) {
                throw new Error("Neplatný interval, `od` je větší než `do`");
            }
        } else {
            throw new Error("Zadání polointervalu není povoleno.");
        }
    }

    if (input.c !== undefined) {
        if (input.c === 0) {
            throw new Error("Nulté století.");
        }
    }

// tohle je tu protoze mesice jsou v JS cislovany od 0, ne od jednicky
    if (validationInput.M) {
        validationInput.M = validationInput.M - 1;
    }

    if (!moment(validationInput).isValid()) {
        throw new Error("Chybný formát datace.");
    }
};

/**
 * Provede validaci vstupního řetězce na dataci.
 * Jako chybu validace vrací obecnou chybovou hlášku, protože aktuálně jsme schopni "správně určit" jen některé chyby zápisu a u ostatních jsou chybové hlášky pro uživatele nesrozumitelné.
 * @param řetězec datace
 */
export function validateUnitDate(value?: string): {valid: boolean, message?: string} {
    const validateError = validate(value);
    const isValid = !validateError;
    const result = {
        valid: isValid,
        message: isValid ? null : i18n("global.validation.datation.invalid")
    }
    return result;
}

/**
 * Provede validaci vstupního řetězce na dataci.
 * Jako chybu validace vrací podrobnější hlášky, ale míchané s hláškami, které nejsou uživateli úplně srozumitelné.
 * @param v řetězec datace
 */
function validate(v?: string) {
    if (v) {
        try {
            const parseResult = parse(v);
            validateUnitDateInput(parseResult)
        } catch (e) {
            return e.message;
        }
    }
}

export const convertToEstimate = (value: string) => {
    const {from, to, c, estimate} = parse(value) || {};

    // from and to are century and one or both are not marked as estimate
    if (from?.c && to?.c && (!from?.estimate || !to?.estimate)){
        const parts = value.replace("[","").replace("]","").split("-");
        return `${parts[0]}/${parts[1]}`;
    }
    // from is century and is not marked as estimate
    else if(from?.c && !from?.estimate){
        const parts = value.split("-");
        return `[${parts[0]}]-${parts[1]}`;
    }
    // to is century and is not marked as estimate
    else if (to?.c && !to?.estimate){
        const parts = value.split("-");
        return `${parts[0]}-[${parts[1]}]`;
    }
    // is century and is not marked as estimate
    else if (c && !estimate){
        return `[${value}]`;
    }

    return value;
}

/**
* Shows a confirmation dialog, when the value meets the right criteria (century not formatted as estimate). 
* If confirmed, converts the value to an estimate format.
* If canceled, returns undefined.
*/
export const convertToEstimateWithConfirmation = async (value: string, dispatch: any) => {
    const {from, to, c, estimate} = parse(value) || {};
    const getResult = async () => await dispatch(showYesNoDialog(i18n("field.unitdate.convertToEstimate.message"), i18n("field.unitdate.convertToEstimate.title")));

    if(
        (from?.c && !from?.estimate)
            || (to?.c && !to?.estimate)
            || (c && !estimate)
    ){
        const result = await getResult();
        if(result === YesNoDialogResult.CANCEL){return undefined;}
        if(result === YesNoDialogResult.YES){
            const newValue = convertToEstimate(value)
            return newValue
        }
    }
    return value;
}

type Props = {
    name: string;
};

const UnitdateField: ForwardRefExoticComponent<Props> = forwardRef(({name, ...rest}, ref) => {
    return <OverlayTrigger
        placement={'right'}
        overlay={
            <Tooltip id={`${name}-tooltip`}>
                <b>Formát datace</b><br/>Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br/>Rok:
                1968<br/>Měsíc: 8.1968<br/>Den: 21.8.1968<br/>Hodiny, minuty, sekundy: 21.8.1968
                2:43 <i>nebo</i> 21.8.1968 8:23:31<br/><b>Intervaly</b><br/>Roky: 1968-1969<br/>Kombinace:
                8.1968-1969 <i>nebo</i> 21.8.1968 2:43-27.6.1969<br/><b>Odhad</b><br/>Definuje se uzavřením hodnoty do kulatých nebo
                hranatých závorek:<br/>Např.: [16.8.1977] <i>nebo</i> [1990]-1992<br/>Při použití znaku "/" pro oddělení intervalu jsou od i do
                chápány jako odhad:<br/>Např.: 1985/1990<br/><b>Záporná datace</b><br/>Definuje se prefixem: <b>-</b> [znaménko minus]<br/>Např.:
                -200 <i>nebo</i> -5000-1.st.<br/>
            </Tooltip>
        }
    >
        <FormInput {...rest} ref={ref}/>
    </OverlayTrigger>;
});

export default UnitdateField;
