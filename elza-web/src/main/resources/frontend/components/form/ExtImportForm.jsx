import React from 'react';
import ReactDOM from 'react-dom';

import {reduxForm} from 'redux-form'
import {Form, Button, FormControl, Table, Modal, OverlayTrigger, Tooltip, Checkbox, ControlLabel} from 'react-bootstrap'
import {FormInput, Icon, HorizontalLoader} from 'components/shared';
import objectById from '../../shared/utils/objectById'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx';
import {submitForm} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {registryDetailFetchIfNeeded} from 'actions/registry/registry.jsx'
import {routerNavigate} from 'actions/router.jsx'
import Scope from '../../components/shared/scope/Scope';
import {apExtSystemListFetchIfNeeded} from 'actions/registry/apExtSystemList';
import {AP_EXT_SYSTEM_TYPE} from '../../constants.tsx';
import ExtMapperForm from "./ExtMapperForm";
import AbstractReactComponent from "../AbstractReactComponent";
import i18n from "../i18n";



const CONDITION_TYPE = {
    AND: "AND",
    OR: "OR"
};

const CONDITIONS = [
    {val: CONDITION_TYPE.AND, name: i18n('extImport.condition.and')},
    {val: CONDITION_TYPE.OR, name: i18n('extImport.condition.or')}
];

const ATTRIBUTE_TYPE = {
    PREFFERED_NAME: "PREFFERED_NAME",
    ALL_NAMES: "ALL_NAMES",
    ID: "ID",
};

const ATTRIBUTE_TYPES = [
    {val: ATTRIBUTE_TYPE.PREFFERED_NAME, name: i18n('extImport.attType.PREFFERED_NAME')},
    {val: ATTRIBUTE_TYPE.ALL_NAMES, name: i18n('extImport.attType.ALL_NAMES')},
    {val: ATTRIBUTE_TYPE.ID, name: i18n('extImport.attType.ID')},
];

class ExtImportSearch extends AbstractReactComponent {
    validate = (values, props) => {
        const {extSystems} = props;
        let errors = {};

        if (!values.systemId) {
            errors.systemId = i18n('global.validation.required');
        } else {
            const sys = objectById(extSystems, values.systemId);
        }

        if (values.conditions) {
            errors.conditions = [];
            values.conditions.forEach((i,index) => {
                if (!i.condition) {
                    if (!errors.conditions[index]) {
                        errors.conditions[index] = {}
                    }
                    errors.conditions[index].condition = i18n('global.validation.required');
                }
                if (!i.attType) {
                    if (!errors.conditions[index]) {
                        errors.conditions[index] = {}
                    }
                    errors.conditions[index].attType = i18n('global.validation.required');
                }
                if (!i.value) {
                    if (!errors.conditions[index]) {
                        errors.conditions[index] = {}
                    }
                    errors.conditions[index].value = i18n('global.validation.required');
                }
            });

            if (errors.conditions.length == 0) {
                delete errors.conditions;
            }
        }

        if (!values.conditions || values.conditions.length < 1) {
            errors._error = i18n('extImport.validation.missingConditions')
        }

        return errors;
    };

    componentDidMount() {
        this.props.dispatch(apExtSystemListFetchIfNeeded());
        if (this.props.extSystems !== null && this.props.extSystems.length === 1) {
            this.props.fields.systemId.onChange(this.props.extSystems[0].id);
        }
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(apExtSystemListFetchIfNeeded());
        if (this.props.extSystems === null && nextProps.extSystems !== null && nextProps.extSystems.length === 1) {
            this.props.fields.systemId.onChange(nextProps.extSystems[0].id);
        }
    }

    renderParam = (fields, index, self) => {
        const {value, attType} = fields;
        return <div className="flex" style={{marginBottom: '10px'}} key={index}>
            <div className="flex-1">
                <FormInput componentClass="select" {...attType} >
                    {ATTRIBUTE_TYPES.map((i,x) => <option key={x} value={i.val}>{i.name}</option>)}
                </FormInput>
            </div>
            <div className="flex-1">
                <FormInput type="text" {...value}/>
            </div>
            <Button bsStyle="action" onClick={()=>self.removeField(index)}><Icon glyph="fa-times"/></Button>
        </div>
    };

    submitOptions = {closeOnFinished:false}

    submitReduxForm = (values, dispatch) => submitForm(this.validate,values,this.props,this.props.onSubmitForm,dispatch,this.submitOptions);

    render() {
        const {fields: {systemId, conditions}, handleSubmit, submitting, error, extSystems} = this.props;

        if (!extSystems) {
            return <HorizontalLoader />
        }
        let system = null;
        if (systemId.value != null) {
            system = objectById(extSystems, systemId.value);
        }

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            {error && <p className="text-danger">{error}</p>}
            <FormInput componentClass="select" label={i18n('extImport.source')} {...systemId}>
                {extSystems.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
            </FormInput>
            {system != null && system && <div>
               <div>{i18n('extImport.notImplemented', system.name)}</div>
            </div>}
            <div className="text-right">
                <Button bsStyle="default" type="submit" disabled={submitting}>{i18n('extImport.search')}</Button>
            </div>
        </Form>
    }
}

const ExtImportSearchComponent = reduxForm({
    fields: ['systemId', 'conditions[].condition', 'conditions[].value', 'conditions[].attType'],
    form: 'extImportSearch'
}, (state, props) => ({
    extSystems: state.app.apExtSystemList.fetched ? state.app.apExtSystemList.rows : null,
    initialValues: {conditions:[{condition: CONDITION_TYPE.AND, value:props.firstValue || null, attType: ATTRIBUTE_TYPES[0].val}]}
}))(ExtImportSearch);


class ExtImportForm extends AbstractReactComponent {
    state = {
        systemId: null,
        searched: false,
        results: [],
    };

    static PropTypes = {
        autocomplete: React.PropTypes.bool,
        isParty: React.PropTypes.bool,
        count: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    static defaultProps = {
        isParty: false,
        count: 200,
        versionId: -1
    };

    submit = (data) => {
        const {results, systemId} = this.state;

        const record = objectById(results, data.interpiRecordId, 'recordId');

        let update = false;
        let recordId = null;

        if (record.pairedRecords) {
            for (let pairedRec of record.pairedRecords) {
                if (pairedRec.scope.id == data.scopeId) {
                    update = true;
                    recordId = pairedRec.recordId;
                    break;
                }
            }
        }
    };

    componentWillReceiveProps(nextProps) {

    }

    submitSearch = (data) => {
        const {isParty, count} = this.props;

        return null;
    };

    showDetail = (detailId) => {
        const {isParty} = this.props;
        if (isParty) {
            this.dispatch(partyDetailFetchIfNeeded(detailId));
            this.dispatch(modalDialogHide());
            this.dispatch(routerNavigate('party'));
        } else {
            this.dispatch(registryDetailFetchIfNeeded(detailId));
            this.dispatch(modalDialogHide());
            this.dispatch(routerNavigate('registry'));
        }
    };

    validate = (values,props) => {
        var errors = {};
        return errors;
    }

    submitOptions = {closeOnFinished:false}

    submitReduxForm = (values, dispatch) => submitForm(this.validate,values,this.props,this.submit,dispatch,this.submitOptions);

    render() {
        return <div>
            <Modal.Body>
                <ExtImportSearchComponent onSubmitForm={this.submitSearch}/>
            </Modal.Body>
        </div>
    }
}

export default reduxForm({
    fields: ['scopeId', 'interpiRecordId', 'originator'],
    form: 'extImportForm'
})(ExtImportForm);

