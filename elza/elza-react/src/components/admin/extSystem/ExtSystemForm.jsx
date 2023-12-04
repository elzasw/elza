import React from 'react';

import {Field, formValueSelector, reduxForm} from 'redux-form';
import {Form, Modal} from 'react-bootstrap';
import {Button} from 'components/ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import {AbstractReactComponent, i18n} from 'components';
import {connect} from 'react-redux';
import {FormInputField} from 'components/shared';
import {JAVA_ATTR_CLASS, GisSystemType, AP_EXT_SYSTEM_TYPE} from '../../../constants';
import {WebApi} from 'actions/index.jsx';

export const EXT_SYSTEM_CLASS = {
    ApExternalSystem: '.ApExternalSystemVO',
    ArrDigitalRepository: '.ArrDigitalRepositoryVO',
    ArrDigitizationFrontdesk: '.ArrDigitizationFrontdeskVO',
    GisExternalSystem: '.GisExternalSystemVO',
};

export const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n('admin.extSystem.class.ApExternalSystemVO'),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n('admin.extSystem.class.ArrDigitalRepositoryVO'),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n('admin.extSystem.class.ArrDigitizationFrontdeskVO'),
    [EXT_SYSTEM_CLASS.GisExternalSystem]: i18n('admin.extSystem.class.GisExternalSystemVO'),
};

export const AP_EXT_SYSTEM_LABEL = {
    [AP_EXT_SYSTEM_TYPE.CAM]: i18n('admin.extSystem.cam'),
    [AP_EXT_SYSTEM_TYPE.CAM_UUID]: i18n('admin.extSystem.cam-uuid'),
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE]: i18n('admin.extSystem.cam-complete'),
};

export const GIS_SYSTEM_TYPE_LABEL = {
    [GisSystemType.FrameApiView]: i18n('admin.extSystem.gis-view'),
    [GisSystemType.FrameApiEdit]: i18n('admin.extSystem.gis-edit'),
}

const FIELDS = {
    abstractExtSystem: [JAVA_ATTR_CLASS, 'id', 'code', 'name', 'url', 'username', 'password', 'elzaCode'],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: ['type', 'apiKeyId', 'apiKeyValue', 'publishOnlyApproved', 'userInfo'],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['viewDaoUrl', 'viewFileUrl', 'viewThumbnailUrl', 'sendNotification'],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [],
    [EXT_SYSTEM_CLASS.GisExternalSystem]: ['type', 'apiKeyId', 'apiKeyValue'],
};

const REQUIRED_FIELDS = {
    abstractExtSystem: [JAVA_ATTR_CLASS, 'code', 'name'],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: ['type', 'apiKeyId', 'apiKeyValue', 'url'],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['sendNotification'],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [],
    [EXT_SYSTEM_CLASS.GisExternalSystem]: ['type', 'url'],
};

class ExtSystemForm extends AbstractReactComponent {
    static fields = [
        ...FIELDS.abstractExtSystem,
        ...FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem],
        ...FIELDS[EXT_SYSTEM_CLASS.GisExternalSystem],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk],
    ];

    static state = {
        defaultScopes: [],
    };

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
        } else if (classJ === EXT_SYSTEM_CLASS.GisExternalSystem) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.GisExternalSystem]);
        }
        return ExtSystemForm.requireFields(...requiredFields)(values);
    };

    componentDidMount() {
        WebApi.getAllScopes().then(json => {
            this.setState({
                defaultScopes: json,
            });
        });
    }

    optionScopes() {
        if (this.state != null) {
            return <>
                <option key={null} />
                {Object.values(this.state.defaultScopes).map((i, index) => (
                    <option key={index} value={i.id}>
                        {i.name}
                    </option>
                ))}
            </>
        }
    }

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
                            <Field
                                name="scope"
                                type="select"
                                component={FormInputField}
                                label={i18n('admin.extSystem.sysScope')}
                            >
                                {this.optionScopes()}
                            </Field>
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                        <div>
                            <Field
                                name="type"
                                type="select"
                                component={FormInputField}
                                label={i18n('admin.extSystem.type')}
                                disabled={isUpdate}
                            >
                                <option key={null} />
                                {Object.values(GisSystemType).map((i, index) => (
                                    <option key={index} value={i}>
                                        {GIS_SYSTEM_TYPE_LABEL[i]}
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
                    {classJ !== EXT_SYSTEM_CLASS.ApExternalSystem
                        && classJ !== EXT_SYSTEM_CLASS.GisExternalSystem
                        && (<>
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
                        </>)}
                    {
                        classJ !== EXT_SYSTEM_CLASS.ApExternalSystem
                        && classJ !== EXT_SYSTEM_CLASS.GisExternalSystem
                        && (
                        <Field
                            name="elzaCode"
                            type="text"
                            component={FormInputField}
                            label={i18n('admin.extSystem.elzaCode')}
                        />
                    )}
                    {(classJ === EXT_SYSTEM_CLASS.ApExternalSystem
                        || classJ === EXT_SYSTEM_CLASS.GisExternalSystem)
                        && (
                            <>
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
                            </>
                        )}
                    {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                        <>
                    <div title={i18n('admin.extSystem.userInfo.title')}>
                        <Field
                            name="userInfo"
                            type="text"
                            component={FormInputField}
                            label={i18n('admin.extSystem.userInfo')}
                        />
                    </div>
                    <Field
                            name="publishOnlyApproved"
                            type="select"
                            component={FormInputField}
                            label={i18n('admin.extSystem.publishOnlyApproved')}
                        >
                            <option key={null} />
                            <option key="true" value={true}>
                                {i18n('admin.extSystem.publishOnlyApproved.true')}
                            </option>
                            <option key="false" value={false}>
                                {i18n('admin.extSystem.publishOnlyApproved.false')}
                            </option>
                    </Field>
                    </>
                    )}
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
