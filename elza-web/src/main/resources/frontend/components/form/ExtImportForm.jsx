import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form'
import {Form, Button, FormControl, Table, Modal, OverlayTrigger, Tooltip, Checkbox} from 'react-bootstrap'
import {AbstractReactComponent, FormInput, i18n, Icon, Loading, ExtMapperForm} from '../index.jsx';
import objectById from '../../shared/utils/objectById'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx';
import {submitForm} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {registrySelect} from 'actions/registry/registryRegionList.jsx'
import {registryRegionDataSelectRecord} from 'actions/registry/registryRegionData.jsx'
import {routerNavigate} from 'actions/router.jsx'
import Scope from '../../components/shared/scope/Scope';
import {regExtSystemListFetchIfNeeded} from 'actions/registry/regExtSystemList';


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

    validate = (values, props) => {
        const {extSystems} = props;
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

        if (!values.conditions || values.conditions.length < 1) {
            errors._error = i18n('extImport.validation.missingConditions')
        }


        return errors;
    };

    componentDidMount() {
        this.props.dispatch(regExtSystemListFetchIfNeeded());
        if (this.props.extSystems !== null && this.props.extSystems.length === 1) {
            this.props.fields.systemId.onChange(this.props.extSystems[0].id);
        }
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(regExtSystemListFetchIfNeeded());
        if (this.props.extSystems === null && nextProps.extSystems !== null && nextProps.extSystems.length === 1) {
            this.props.fields.systemId.onChange(nextProps.extSystems[0].id);
        }
    }

    renderParam = (fields, index, self) => {
        const {value, attType} = fields;
        return <div className="flex" key={index}>
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
        const {fields: {systemId, conditions}, handleSubmit, submitting, error, extSystems} = this.props;

        if (!extSystems) {
            return <Loading />
        }
        let system = null;
        if (systemId.value != null) {
            system = objectById(extSystems, systemId.value);
        }
        const submit = submitForm.bind(this, this.validate);

        return <Form onSubmit={handleSubmit(submit)}>
            {error && <p className="text-danger">{error}</p>}
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
    extSystems: state.app.regExtSystemList.fetched ? state.app.regExtSystemList.data : null,
    initialValues: {params:[{condition:null, value:props.firstValue || null, attType: null}]}
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
        versionId: null
    };

    submit = (data) => {
        const {results, systemId} = this.state;
        const {isParty} = this.props;
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

        const importVO = {...data, scopeId: parseInt(data.scopeId), systemId: parseInt(systemId)};

        const send = (data, update, recordId = null) => {
            const promise = update ? WebApi.importRecordUpdate(recordId, data) : WebApi.importRecord(data);

            promise.then(e => {
                this.dispatch(modalDialogHide());
                this.dispatch(addToastrSuccess(i18n("extImport.done.title"), i18n(update ? "extImport.done.messageImport" : "extImport.done.messageUpdate")));
                this.props.onSubmitForm && this.props.onSubmitForm(e);
            });

            return promise;
        };

        if (importVO.originator) {
            return WebApi.findInterpiRecordRelations(importVO.interpiRecordId, importVO.systemId).then(mapping => {
                this.dispatch(modalDialogHide());
                this.dispatch(modalDialogShow(this, i18n('extMapperForm.title'), <ExtMapperForm
                    initialValues={mapping}
                    record={record}
                    onSubmit={(data) => {
                        importVO.mapping = data.mapping;
                        return send(importVO, update, recordId);
                    }
                } />, "dialog-lg"));
            });
        } else {
            return send(importVO, update, recordId);
        }
    };

    componentWillReceiveProps(nextProps) {
        if (this.props.fields.interpiRecordId.value !== nextProps.fields.interpiRecordId.value && nextProps.fields.interpiRecordId.value) {
            const {results} = this.state;
            const record = objectById(results, nextProps.fields.interpiRecordId.value, 'recordId');
            if (record && record.pairedRecords.length === 1) {
                this.props.fields.scopeId.onChange(record.pairedRecords[0].scope.id);
            }
        }
    }

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
            this.dispatch(registryRegionDataSelectRecord({selectedId: detailId}));
            this.dispatch(modalDialogHide());
            this.dispatch(routerNavigate('registry'));
        }
    };

    render() {
        const {searched, results} = this.state;
        const {autocomplete, onClose, fields:{scopeId, interpiRecordId, originator}, handleSubmit, submitting, versionId} = this.props;

        let record = null;
        if (interpiRecordId.value) {
            record = objectById(results, interpiRecordId.value, 'recordId')
        }

        let showDetail = false;
        let detailId = null;

        if (record && scopeId.value) {
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
                                                <th>{i18n('extImport.alreadyImported')}</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                                {results.map(i => <tr className={record && record.recordId == i.recordId ? "active" : null} onClick={() => interpiRecordId.onChange(i.recordId)}>
                                                    <td>{i.recordId}</td>
                                                    <td>{i.name}</td>
                                                    <td>{i.pairedRecords && i.pairedRecords.length > 0 ? <OverlayTrigger overlay={<Tooltip id='tt'>{i.pairedRecords.map((x,index) => (index != 0 ? ', ' : '') + x.scope.name)}</Tooltip>} placement="top">
                                                        <Icon glyph="fa-check" />
                                                    </OverlayTrigger> : null}</td>
                                                </tr>)}
                                            </tbody>
                                        </Table>
                                    </div>
                                </div>
                                <div className="flex-1">
                                    <label>{i18n('extImport.resultDetail')}</label>
                                    <div>
                                        <FormControl componentClass="textarea" rows="10" value={record ? record.detail : ''} style={{height: '272px'}} />
                                    </div>
                                </div>
                            </div>
                            <div>
                                <div>
                                    <Scope label={i18n('extImport.scopeId')} {...scopeId} versionId={versionId} />
                                </div>
                                <div>
                                    <Checkbox {...originator}>
                                        {i18n('extImport.originator')}
                                    </Checkbox>
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
    fields: ['scopeId', 'interpiRecordId', 'originator'],
    form: 'extImportForm'
})(ExtImportForm);

