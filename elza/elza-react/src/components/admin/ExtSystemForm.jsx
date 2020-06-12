import React from 'react';

import {Field, formValueSelector, reduxForm} from 'redux-form';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import i18n from '../i18n';
import AbstractReactComponent from '../AbstractReactComponent';
import {connect} from 'react-redux';
import {FormInputField} from 'components/shared';
import {AP_EXT_SYSTEM_TYPE} from 'constants.tsx';
import {JAVA_ATTR_CLASS} from '../../constants';

const EXT_SYSTEM_CLASS = {
    ApExternalSystem: '.ApExternalSystemVO',
    ArrDigitalRepository: '.ArrDigitalRepositoryVO',
    ArrDigitizationFrontdesk: '.ArrDigitizationFrontdeskVO',
};

const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n('admin.extSystem.class.ApExternalSystemVO'),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n('admin.extSystem.class.ArrDigitalRepositoryVO'),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n('admin.extSystem.class.ArrDigitizationFrontdeskVO'),
};

const AP_EXT_SYSTEM_LABEL = {
    [AP_EXT_SYSTEM_TYPE.CAM]: i18n('admin.extSystem.cam'),
};

const FIELDS = {
    abstractExtSystem: [JAVA_ATTR_CLASS, 'id', 'code', 'name', 'url', 'username', 'password', 'apiKeyId', 'apiKeyValue', 'elzaCode'],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: ['type'],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['viewDaoUrl', 'viewFileUrl', 'viewThumbnailUrl', 'sendNotification'],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [],
};

const REQUIRED_FIELDS = {
    abstractExtSystem: [JAVA_ATTR_CLASS, 'code', 'name'],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: ['type', 'apiKeyId', 'apiKeyValue', 'url'],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['sendNotification'],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [],
};

class ExtSystemForm extends AbstractReactComponent {
    static fields = [
        ...FIELDS.abstractExtSystem,
        ...FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk],
    ];

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required');
            }
            return errors;
        }, {});

    static validate = (values, props) => {
        const classJ = values[JAVA_ATTR_CLASS];

        let requiredFields = [...REQUIRED_FIELDS.abstractExtSystem];

        if (classJ === EXT_SYSTEM_CLASS.ApExternalSystem) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem]);
        } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository]);
        } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]);
        }
        return ExtSystemForm.requireFields(...requiredFields)(values);
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(ExtSystemForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {id, classJ, handleSubmit, pristine, submitting} = this.props;
        const isUpdate = !!id;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Field
                        name={JAVA_ATTR_CLASS}
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.class')}
                        disabled={isUpdate}
                    >
                        <option key={null} />
                        {Object.values(EXT_SYSTEM_CLASS).map((i, index) => (
                            <option key={index} value={i}>
                                {EXT_SYSTEM_CLASS_LABEL[i]}
                            </option>
                        ))}
                    </Field>

                    {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                        <div>
                            <Field
                                name="type"
                                type="select"
                                component={FormInputField}
                                label={i18n('admin.extSystem.type')}
                                disabled={isUpdate}
                            >
                                <option key={null} />
                                {Object.values(AP_EXT_SYSTEM_TYPE).map((i, index) => (
                                    <option key={index} value={i}>
                                        {AP_EXT_SYSTEM_LABEL[i]}
                                    </option>
                                ))}
                            </Field>
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                        <div>
                            <Field
                                name="viewDaoUrl"
                                type="text"
                                component={FormInputField}
                                label={i18n('admin.extSystem.viewDaoUrl')}
                            />
                            <Field
                                name="viewFileUrl"
                                type="text"
                                component={FormInputField}
                                label={i18n('admin.extSystem.viewFileUrl')}
                            />
                            <Field
                                name="viewThumbnailUrl"
                                type="text"
                                component={FormInputField}
                                label={i18n('admin.extSystem.viewThumbnailUrl')}
                            />
                            <Field
                                name="sendNotification"
                                type="select"
                                component={FormInputField}
                                label={i18n('admin.extSystem.sendNotification')}
                            >
                                <option key={null} />
                                <option key="true" value={true}>
                                    {i18n('admin.extSystem.sendNotification.true')}
                                </option>
                                <option key="false" value={false}>
                                    {i18n('admin.extSystem.sendNotification.false')}
                                </option>
                            </Field>
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && <div />}
                    <Field
                        name="code"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.code')}
                        disabled={isUpdate}
                    />
                    <Field name="name" type="text" component={FormInputField} label={i18n('admin.extSystem.name')} />
                    <Field name="url" type="text" component={FormInputField} label={i18n('admin.extSystem.url')} />
                    <Field
                        name="username"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.username')}
                    />
                    <Field
                        name="password"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.password')}
                    />
                    <Field
                        name="apiKeyId"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.apiKeyId')}
                    />
                    <Field
                        name="apiKeyValue"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.apiKeyValue')}
                    />
                    <Field
                        name="elzaCode"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.elzaCode')}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {isUpdate ? i18n('admin.extSystem.submit.edit') : i18n('admin.extSystem.submit.add')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const selector = formValueSelector('extSystemForm');

const mapState = state => ({
    id: selector(state, 'id'),
    classJ: selector(state, JAVA_ATTR_CLASS),
});
const connector = connect(mapState);

export default reduxForm({
    form: 'extSystemForm',
})(connector(ExtSystemForm));
