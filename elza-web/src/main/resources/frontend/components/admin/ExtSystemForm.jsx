import React from 'react';
import ReactDOM from 'react-dom';

import {reduxForm} from 'redux-form'
import {Form, Button, FormControl, Table, Modal, OverlayTrigger, Tooltip, Checkbox} from 'react-bootstrap'
import {submitForm} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {extSystemDetailFetchIfNeeded} from 'actions/admin/extSystem.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {extSystemListFetchIfNeeded} from 'actions/admin/extSystem.jsx';
import {AP_EXT_SYSTEM_TYPE} from '../../constants.tsx';
import i18n from "../i18n";
import AbstractReactComponent from "../AbstractReactComponent";
import {FormInput} from "../shared/index";

const EXT_SYSTEM_CLASS = {
    ApExternalSystem: ".ApExternalSystemVO",
    ArrDigitalRepository: ".ArrDigitalRepositoryVO",
    ArrDigitizationFrontdesk: ".ArrDigitizationFrontdeskVO"
};

const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n("admin.extSystem.class.ApExternalSystemVO"),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n("admin.extSystem.class.ArrDigitalRepositoryVO"),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n("admin.extSystem.class.ArrDigitizationFrontdeskVO"),
};

const FIELDS = {
    abstractExtSystem: [
        '@class',
        'id',
        'code',
        'name',
        'url',
        'username',
        'password',
        'elzaCode'
    ],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: [
        'type',
    ],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: [
        'viewDaoUrl',
        'viewFileUrl',
        'viewThumbnailUrl',
        'sendNotification'
    ],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [
    ]
};

const REQUIRED_FIELDS = {
    abstractExtSystem: [
        '@class',
        'code',
        'name',
    ],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: [
        'type'
    ],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: [
        'sendNotification'
    ],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [
    ]
};

class ExtSystemForm extends AbstractReactComponent {

    static fields = [
        ...FIELDS.abstractExtSystem,
        ...FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository],
        ...FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]
    ];

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});


    static validate = (values, props) => {
        const classJ = values["@class"];

        let requiredFields = [...REQUIRED_FIELDS.abstractExtSystem];

        if(classJ == EXT_SYSTEM_CLASS.ApExternalSystem) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem])
        }else if(classJ == EXT_SYSTEM_CLASS.ArrDigitalRepository) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository])
        }else if(classJ == EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk])
        }
        return ExtSystemForm.requireFields(...requiredFields)(values);
    };

    submitReduxForm = (values, dispatch) => submitForm(ExtSystemForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {id, type, viewDaoUrl, viewFileUrl, viewThumbnailUrl, sendNotification, code, name, url, username, password, elzaCode}, handleSubmit, submitting} = this.props;
        const classJ = this.props.fields['@class'];
        const isUpdate = !!id.value;

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body>
                <FormInput componentClass="select" label={i18n('admin.extSystem.class')} {...classJ} disabled={id.value}>
                    <option key={null}/>
                    {Object.values(EXT_SYSTEM_CLASS).map((i, index) => <option key={index}
                                                                               value={i}>{EXT_SYSTEM_CLASS_LABEL[i]}</option>)}
                </FormInput>
                {classJ.value == EXT_SYSTEM_CLASS.ApExternalSystem && <div>
                    <FormInput componentClass="select" label={i18n('admin.extSystem.type')} {...type} disabled={id.value}>
                        <option key={null}/>
                        <option value={AP_EXT_SYSTEM_TYPE.INTERPI}>{i18n('admin.extSystem.interpi')}</option>
                    </FormInput>
                </div>}
                {classJ.value == EXT_SYSTEM_CLASS.ArrDigitalRepository && <div>
                    <FormInput type="text" label={i18n('admin.extSystem.viewDaoUrl')} {...viewDaoUrl} />
                    <FormInput type="text" label={i18n('admin.extSystem.viewFileUrl')} {...viewFileUrl} />
                    <FormInput type="text" label={i18n('admin.extSystem.viewThumbnailUrl')} {...viewThumbnailUrl} />
                    <FormInput componentClass="select" label={i18n('admin.extSystem.sendNotification')} {...sendNotification} >
                        <option key={null} />
                        <option key="true" value={true}>{i18n('admin.extSystem.sendNotification.true')}</option>
                        <option key="false" value={false}>{i18n('admin.extSystem.sendNotification.false')}</option>
                    </FormInput>
                </div>}
                {classJ.value == EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && <div></div>}
                <FormInput type="text" label={i18n('admin.extSystem.code')} {...code} disabled={id.value}/>
                <FormInput type="text" label={i18n('admin.extSystem.name')} {...name} />
                <FormInput type="text" label={i18n('admin.extSystem.url')} {...url} />
                <FormInput type="text" label={i18n('admin.extSystem.username')} {...username} />
                <FormInput type="text" label={i18n('admin.extSystem.password')} {...password} />
                <FormInput type="text" label={i18n('admin.extSystem.elzaCode')} {...elzaCode} />


            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" bsStyle="default" disabled={submitting}>{isUpdate ? i18n('admin.extSystem.submit.edit') : i18n('admin.extSystem.submit.add')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    fields: ExtSystemForm.fields,
    form: 'extSystemForm'
})(ExtSystemForm);
