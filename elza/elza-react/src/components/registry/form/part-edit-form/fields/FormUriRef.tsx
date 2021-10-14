import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import {Col, Row} from 'react-bootstrap';
import FormInput from '../../../../shared/form/FormInput';

export const FormUriRef:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Row>
        <Col xs={6}>
            <Field
                name={`${name}.value`}
                label={label}
                disabled={disabled}
                maxLength={1000}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                />
        </Col>
        <Col xs={6}>
            <Field
                name={`${name}.description`}
                label="Název odkazu"
                disabled={disabled}
                maxLength={250}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                />
        </Col>
    </Row>
}
