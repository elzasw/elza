import * as React from 'react';
import {InjectedFormProps, reduxForm, FormErrors, Form as RForm, Field, DecoratedFormProps} from 'redux-form';
import {ArrRefTemplateVO} from '../../types';
import {Form, Modal, Button} from 'react-bootstrap';
import FormInputField from '../shared/form/FormInputField';
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';
import { templateMessages } from './templateMessages';
import DescItemTypeField from './DescItemTypeField';
import FF from '../shared/form/FF';

type OwnProps = {};
type Props = OwnProps & InjectedFormProps<ArrRefTemplateVO, OwnProps, FormErrors<ArrRefTemplateVO>>;

class ArrRefTemplateForm extends React.Component<Props> {
    render() {
        const {handleSubmit, pristine, submitting} = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <Field
                        name="name"
                        type="text"
                        component={FormInputField}
                        label={<FormattedMessage {...templateMessages.refTemplatesDetailName} />}
                    />
                    <FF
                        name="itemTypeId"
                        field={DescItemTypeField}
                        label={<FormattedMessage {...templateMessages.refTemplatesDetailItemTypeId} />}
                        useIdAsValue
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {<FormattedMessage {...globalMessages.save} />}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm<ArrRefTemplateVO, OwnProps, FormErrors<ArrRefTemplateVO>>({
    form: 'ArrRefTemplateForm',
    validate(
        values: ArrRefTemplateVO,
        props: DecoratedFormProps<ArrRefTemplateVO, OwnProps, FormErrors<ArrRefTemplateVO>>,
    ): FormErrors<ArrRefTemplateVO, FormErrors<ArrRefTemplateVO>> {
        const errors: Record<string, string> = {};
        if (!values.name) {
            errors.name = getIntl().formatMessage(globalMessages.validationRequired);
        }
        if (!values.itemTypeId) {
            errors.itemTypeId = getIntl().formatMessage(globalMessages.validationRequired);
        }
        return errors as FormErrors<ArrRefTemplateVO, FormErrors<ArrRefTemplateVO>>;
    },
})(ArrRefTemplateForm);
