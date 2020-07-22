import * as React from 'react';
import {InjectedFormProps, reduxForm, FormErrors, Field, DecoratedFormProps} from 'redux-form';
import {ArrRefTemplateVO} from '../../types';
import {Form, Modal, Button, Col} from 'react-bootstrap';
import FormInputField from '../shared/form/FormInputField';
import i18n from '../i18n';
import RefTemplateField from './RefTemplateField';
import FF from '../shared/form/FF';
import {WebApi} from '../../actions/WebApi';
import {modalDialogHide} from '../../actions/global/modalDialog';
import {Dispatch} from 'redux';

type OwnProps = {
    fundId: number;
    nodeId: number;
    nodeVersion: number;
};
type FormData = {
    templateId: number;
    childrenNodes: boolean;
};
type Props = OwnProps & InjectedFormProps<FormData, OwnProps, FormErrors<FormData>>;

class SyncNodes extends React.Component<Props> {
    render() {
        const {handleSubmit, pristine, submitting, fundId} = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <FF
                        name="templateId"
                        field={RefTemplateField}
                        label={i18n('arr.syncNodes.templateId')}
                        fundId={fundId}
                        useIdAsValue
                    />
                    <Field
                        name="childrenNodes"
                        type="checkbox"
                        component={FormInputField}
                        label={i18n('arr.syncNodes.childrenNodes')}
                        disabled={submitting}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {i18n('global.action.run')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm<FormData, OwnProps, FormErrors<FormData>>({
    form: 'SyncNodesForm',
    validate(
        values: FormData,
        props: DecoratedFormProps<FormData, OwnProps, FormErrors<FormData>>,
    ): FormErrors<FormData, FormErrors<FormData>> {
        const errors: FormErrors<FormData, FormErrors<FormData>> = {};
        if (!values.templateId) {
            errors.templateId = i18n('global.validation.required');
        }
        return errors;
    },
    initialValues: {
        templateId: undefined,
        childrenNodes: false,
    },
    onSubmit: (data, dispatch, props) => {
        return WebApi.synchronizeNodes(props.nodeId, props.nodeVersion, data.templateId, data.childrenNodes);
    },
    onSubmitSuccess(
        result: any,
        dispatch: Dispatch<any>,
        props: DecoratedFormProps<FormData, OwnProps, FormErrors<FormData>>,
    ) {
        dispatch(modalDialogHide());
    },
})(SyncNodes);
