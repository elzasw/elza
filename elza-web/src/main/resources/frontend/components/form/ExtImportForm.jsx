import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form'
import {Form, Button, FormControl, Table, Modal} from 'react-bootstrap'
import {AbstractReactComponent, FormInput, i18n, Icon, Loading} from '../index.jsx';
import objectById from '../../shared/utils/objectById'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx';
import {submitReduxForm} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {registrySelect} from 'actions/registry/registryRegionList.jsx'
import {routerNavigate} from 'actions/router.jsx'


const EXT_SYSTEM_CODE_INTERPI = 'INTERPI';

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
};

const ATTRIBUTE_TYPES = [
    {val: ATTRIBUTE_TYPE.PREFFERED_NAME, name: i18n('extImport.attType.PREFFERED_NAME')},
    {val: ATTRIBUTE_TYPE.ALL_NAMES, name: i18n('extImport.attType.ALL_NAMES')},
];

class ExtImportSearch extends AbstractReactComponent {
    state = {extSystems: null};

    validate = (values) => {
        const {extSystems} = this.state
        let errors = {};

        if (!values.systemId) {
            errors.systemId = i18n('global.validation.required');
        } else {
            const sys = objectById(extSystems, values.systemId);
            if (sys.type !== EXT_SYSTEM_CODE_INTERPI) {
                errors.systemId = i18n('extImport.validation.notInterpi');
            }
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


        return errors;
    };

    componentDidMount() {
        WebApi.getRegExternalSystems().then((data) => {
            if (data && data.length === 1) {
                this.props.fields.systemId.onChange(data[0].id);
            }
            this.setState({extSystems: data})
        })
    }

    renderParam = (fields, index, self) => {
        const {condition, value, attType} = fields;
        return <div className="flex" key={index}>
            {/*index !== 0 && <div className="flex-1">
                <FormInput componentClass="select" {...condition} label={i18n('extImport.condition')}>
                    <option key="null" />
                    {CONDITIONS.map((i,x) => <option key={x} value={i.val}>{i.name}</option>)}
                </FormInput>
            </div>*/}
            <div className="flex-1">
                <FormInput componentClass="select" {...attType} label={i18n('extImport.attType')}>
                    <option key="null" />
                    {ATTRIBUTE_TYPES.map((i,x) => <option key={x} value={i.val}>{i.name}</option>)}
                </FormInput>
            </div>
            <div className="flex-1">
                <FormInput type="text" {...value} label={i18n('extImport.value')}/>
            </div>
            <Button bsStyle="action" style={{marginTop:'25px'}} onClick={()=>self.removeField(index)}><Icon glyph="fa-times"/></Button>
        </div>
    };

    render() {
        const {fields: {systemId, conditions}, handleSubmit, submitting} = this.props;
        const {extSystems} = this.state;

        if (!extSystems) {
            return <Loading />
        }
        let system = null;
        if (systemId.value != null) {
            system = objectById(extSystems, systemId.value);
        }
        const submit = submitReduxForm.bind(this, this.validate);

        return <Form onSubmit={handleSubmit(submit)}>
            <FormInput componentClass="select" label={i18n('extImport.source')} {...systemId}>
                <option key="null" />
                {extSystems.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
            </FormInput>
            {system != null && system && <div>
                {system.type === EXT_SYSTEM_CODE_INTERPI ? <div>
                    <label>Hledané parametry</label>
                    {conditions.map(this.renderParam)}
                    <Button bsStyle="action" onClick={() => conditions.addField({condition: CONDITION_TYPE.AND, value:null, attType: null})}>
                        <Icon glyph="fa-plus" /></Button>
                </div> : <div>{i18n('extImport.notImplemented', system.name)}</div>}
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
    initialValues: {params:[{condition:null, value:props.firstValue || null, attType: null}]}
}))(ExtImportSearch);


class ExtImportForm extends AbstractReactComponent {
    state = {
        systemId: null,
        searched: false,
        results: [],
        scopes: [],
    };

    static PropTypes = {
        autocomplete: React.PropTypes.bool,
        isParty: React.PropTypes.bool,
        count: React.PropTypes.number
    };

    static defaultProps = {
        isParty: false,
        count: 200
    };

    componentDidMount() {
        console.log(this.props, true)
        WebApi.getAllScopes().then(scopes => {
            if (scopes.length === 1) {
                this.props.fields.scopeId.onChange(scopes[0].id);
            }
            this.setState({scopes});
        });
    }

    submit = (data) => {
        const {results, systemId, isParty} = this.state;
        const record = objectById(results, data.interpiRecordId, 'recordId');

        let update = false;
        let recordId = null;

        if (record.pairedRecords) {
            for (let pairedRec of record.pairedRecords) {
                if (pairedRec.scope.scopeId == data.scopeId) {
                    update = true;
                    recordId = pairedRec.recordId;
                    break;
                }
            }
        }

        const importVO = {...data, scopeId: parseInt(data.scopeId), systemId: parseInt(systemId)};

        const promise = update ? WebApi.importRecordUpdate(recordId, importVO) : WebApi.importRecord(importVO);

        promise.then(e => {
            this.dispatch(modalDialogHide());
            this.dispatch(addToastrSuccess(i18n("extImport.done.title"), i18n(update ? "extImport.done.messageImport" : "extImport.done.messageUpdate")));
        })

        return promise;
    };

    submitSearch = (data) => {
        const {isParty, count} = this.props;

        return WebApi.findInterpiRecords({...data, isParty, count, systemId: parseInt(data.systemId)}).then((results) => {
            this.setState({searched: true, results, selectedRecordId: null, systemId: parseInt(data.systemId)});
        })
    };

    showDetail = (detailId) => {
        const {isParty} = this.props;
        if (isParty) {
            this.dispatch(partyDetailFetchIfNeeded(detailId));
            this.dispatch(modalDialogHide());
            this.dispatch(routerNavigate('party'));
        } else {
            this.dispatch(partyDetailFetchIfNeeded(detailId));
            this.dispatch(modalDialogHide());
            this.dispatch(routerNavigate('registry'));
        }
    };

    render() {
        const {searched, results, scopes} = this.state;
        const {autocomplete, onClose, fields:{scopeId, interpiRecordId}, handleSubmit, submitting} = this.props;

        let record = null;
        if (interpiRecordId.value) {
            record = objectById(results, interpiRecordId.value, 'recordId')
        }

        let showDetail = false;
        let detailId = null;
        let visibleSubmit = false;

        if (record && scopeId.value) {
            visibleSubmit = true;
            if (record.pairedRecords) {
                for (let pairedRec of record.pairedRecords) {
                    if (pairedRec.scope.id == scopeId.value) {
                        showDetail = true;
                        detailId = pairedRec.recordId;
                        break;
                    }
                }
            }
        }
        const disabledSubmit = submitting || !scopeId.value || !interpiRecordId.value;

        return <div>
            <Modal.Body>
                <ExtImportSearchComponent onSubmitForm={this.submitSearch}/>
                <Form name="extImport" onSubmit={handleSubmit(this.submit)}>
                    {searched && <div>
                        <hr />
                        {results && results.length === 0 ? <div>{i18n('extImport.noResults')}</div> : <div>
                            <div className="flex">
                                <div className="flex-2">
                                    <label>{i18n('extImport.results')}</label>
                                    <div style={{height: '272px', overflowY: 'auto'}}>
                                        <Table>
                                            <thead>
                                             <tr>
                                                <th>{i18n('extImport.id')}</th>
                                                <th>{i18n('extImport.record')}</th>
                                                <th>{i18n('extImport.scopeId')}</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                                {results.map(i => <tr className={record && record.recordId == i.recordId ? "active" : null} onClick={() => interpiRecordId.onChange(i.recordId)}>
                                                    <td>{i.recordId}</td>
                                                    <td>{i.name}</td>
                                                    <td>{i.pairedRecords && i.pairedRecords.length > 0 ? i.pairedRecords.map((i,index) => (index != 0 ? ', ' : '') + i.scope.name ) : '-'}</td>
                                                </tr>)}
                                            </tbody>
                                        </Table>
                                    </div>
                                </div>
                                <div className="flex-1">
                                    <label>{i18n('extImport.resultDetail')}</label>
                                    <div>
                                        <FormControl componentClass="textarea" rows="10" disabled={true} value={record ? record.detail : ''} style={{height: '272px'}} />
                                    </div>
                                </div>
                            </div>
                            <div>
                                <div>
                                    <label>{i18n('extImport.scopeId')}</label>
                                    <FormControl componentClass="select" {...scopeId}>
                                        <option key="null" />
                                        {scopes && scopes.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                                    </FormControl>
                                </div>
                            </div>
                        </div>}
                    </div>}
                    {searched && <Modal.Footer>
                        {showDetail ? <span>
                        <Button type="submit" disabled={disabledSubmit}>{i18n('extImport.update')}</Button>
                        <Button type="button" onClick={() => this.showDetail(detailId)} disabled={disabledSubmit}>{autocomplete ? i18n('extImport.useActual') : i18n('extImport.showDetail')}</Button>
                    </span> : <span>
                        <Button type="submit" disabled={disabledSubmit}>{i18n('global.action.import')}</Button>
                    </span>}
                        <Button bsStyle="link" type="button" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>}
                </Form>
            </Modal.Body>
        </div>
    }
}

export default reduxForm({
    fields: ['scopeId', 'interpiRecordId'],
    form: 'extImportForm'
})(ExtImportForm);

