import React, {FC} from 'react';
import moment from "moment";
import {Datace} from "../../shared/datace/datace-types";
import parse from "../../shared/datace/datace";
import {Form, Tooltip} from "react-bootstrap";

type Props = {
    name: string;
};

const validateUnitDateInput = (input: Datace) => {
    const validationInput = {...input};
    if (input.from) {
        if (input.to) {
            validateUnitDateInput(input.from);
            validateUnitDateInput(input.to);
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

export function validate(v?: string) {
    if (v) {
        try {
            const parseResult = parse(v);
            validateUnitDateInput(parseResult)
        } catch (e) {
            return e.message;
        }
    }
}

const UnitdateField: FC<Props> = ({name, ...rest}) => {
    return <Tooltip placement="right" id={`${name}-tooltip`}>
        <div><b>Formát datace</b><br/>Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br/>Rok:
            1968<br/>Měsíc.rok: 8.1968<br/>Datum: 21.8.1698<br/>Datum a čas: 21.8.1968
            8:23 <i>nebo</i> 21.8.1968 8:23:31<br/><b>Intervaly</b><br/>Jednotlivá hodnota: 1968<br/>Interval:
            21.8.1968 0:00-27.6.1989<br/><b>Odhad</b><br/>Definuje se uzavřením hodnoty do kulatých nebo
            hranatých závorek: [16.8.1977]<br/>Při použití znaku "/" pro oddělení intervalu jsou od i do
            chápány jako odhad.<br/><b>Záporná datace</b><br/>Definuje se prefixem: <b>bc</b><br/>Např: bc
            200 <i>nebo</i> bc 5000-1.st.<br/></div>
        <Form.Control {...rest}/>
    </Tooltip>
};

export default UnitdateField;
