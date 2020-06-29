import * as React from 'react';
import {InjectedFormProps, reduxForm, FormErrors, Form as RForm, Field, DecoratedFormProps} from 'redux-form';
import {ArrRefTemplateVO} from '../../types';
import {Form, Modal, Button} from 'react-bootstrap';
import FormInputField from '../shared/form/FormInputField';
import i18n from '../i18n';
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
                        label={i18n('arr.refTemplates.detail.name')}
                    />
                    <FF
                        name="itemTypeId"
                        field={DescItemTypeField}
                        label={i18n('arr.refTemplates.detail.itemTypeId')}
                        useIdAsValue
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {i18n('global.action.update')}
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
        const errors: FormErrors<ArrRefTemplateVO, FormErrors<ArrRefTemplateVO>> = {};
        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }
        if (!values.itemTypeId) {
            errors.itemTypeId = i18n('global.validation.required');
        }
        return errors;
    },
})(ArrRefTemplateForm);
