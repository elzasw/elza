import React, {FC} from 'react';
import classNames from 'classnames';
// import { Field} from 'redux-form';
import { Field } from 'react-final-form';
import {Button, Col, Row} from 'react-bootstrap';
import {Icon} from '../../../../index';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

export const FormCoordinates:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    onImport?: (name: string) => void;
}> = ({
    name,
    label,
    disabled = false,
    onImport = () => console.warn("'onImport' undefined.")
}) => {
    return <Row>
        <Col>
            <Field
                name={`${name}.value`}
                label={label}
                disabled={disabled}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                as={'textarea'}
                />
        </Col>
        <Col xs="auto" className="action-buttons">
            {/*TODO: az bude na serveru */}
            <Button
                variant={'action' as any}
                className={classNames('side-container-button', 'm-1')}
                title={'Importovat'}
                onClick={() => {
                    onImport(`${name}.value`);
                }}
            >
                <Icon glyph={'fa-download'} />
            </Button>
        </Col>
    </Row>
}
