/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Autocomplete, Icon} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet'
import {WebApi} from 'actions'
import {modalDialogHide} from 'actions/global/modalDialog';
import {addToastrDanger, addToastrSuccess} from 'components/shared/toastr/ToastrActions'

const validate = (values, props) => {
    const errors = {};

    if (values.transformationName) {
        if (!values.ruleSetId) {
            errors.ruleSetId = i18n('global.validation.required');
        }
        if (!values.rulArrTypeId) {
            errors.rulArrTypeId = i18n('global.validation.required');
        }

    }

    if (!values.recordScope || !values.recordScope.name) {
        errors.recordScope = i18n('global.validation.required');
    }
    if (!values.xmlFile || values.xmlFile == null) {
        errors.xmlFile = i18n('global.validation.required');
    }
    return errors;
};

var ImportForm = class ImportForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {defaultScopes: [], transformationNames: [], isRunning: false};
        this.bindMethods('save');
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
        if (this.props.fund) {
            WebApi.getAllScopes().then(json => {
                this.setState({
                    defaultScopes: json
                });
            });
        } else {
            WebApi.getDefaultScopes().then(json => {
                this.setState({
                    defaultScopes: json
                });
            });
        }
        WebApi.getTransformations().then(json => {
            this.setState({
                transformationNames: json
            });
        });
    }

    save(values) {
        //validate
        this.setState({
            isRunning: true,
        });
        var data = Object.assign({}, {
            xmlFile: values.xmlFile[0],
            importDataFormat: this.props.fund ? 'FUND' : this.props.record ? 'RECORD' : 'PARTY',
            stopOnError: values.stopOnError && values.stopOnError == 1 ? values.stopOnError : false
        });

        if (values.ruleSetId) {
            data.ruleSetId = values.ruleSetId;
        }

        if (values.rulArrTypeId) {
            data.arrangementTypeId = values.rulArrTypeId;
        }

        if (values.transformationName) {
            data.transformationName = values.transformationName;
        }

        if (values.recordScope) {
            if (values.recordScope.id) {
                data.scopeId = values.recordScope.id;
            }
            if (values.recordScope.name) {
                data.scopeName = values.recordScope.name;
            }
        }

        var formData = new FormData();

        for (var key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        WebApi.xmlImport(formData).then(() => {
            const messageType = this.props.fund ? 'Fund' : this.props.record ? 'Record' : 'Party';
            this.dispatch(modalDialogHide());
            this.dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.success' + messageType)));
        }).catch(() => {
            this.dispatch(modalDialogHide());
        });
    }

    render() {
        const {fields: {ruleSetId, rulArrTypeId, transformationName, recordScope, stopOnError, xmlFile}, onClose, handleSubmit} = this.props;
        var ruleSets = this.props.refTables.ruleSet.items;
        var currRuleSetId = this.props.values.ruleSetId;
        var currRuleSet = [];
        var ruleSetOptions = [];
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
            if (currRuleSet) {
                ruleSetOptions = currRuleSet.arrangementTypes.map(
                    i=> <option key={i.id} value={i.id}>{i.name}</option>
                );
            }
        }
        return (
            <div>
                {
                    !this.state.isRunning && <div>
                        <Modal.Body>
                            <form onSubmit={handleSubmit(this.save)}>
                                {
                                    <div>
                                        <Input type="select"
                                               label={i18n('import.transformationName')} {...transformationName} {...decorateFormField(transformationName)}>
                                            <option key='blankName'/>
                                            {this.state.transformationNames.map((i, index)=> {
                                                return <option key={index+'name'} value={i}>{i}</option>
                                            })}
                                        </Input>
                                        <Autocomplete
                                            {...recordScope}
                                            {...decorateFormField(recordScope)}
                                            tags={this.props.fund == true}
                                            label={i18n('import.registryScope')}
                                            items={this.state.defaultScopes}
                                            getItemId={(item) => item ? item : null}
                                            getItemName={(item) => item ? item.name : ''}
                                            onChange={
                                                (id, value) => {
                                                    if (value.name.trim() == '') {
                                                        return;
                                                    }

                                                    recordScope.onChange({id: value.id, name:value.name});
                                                }
                                            }
                                        />
                                        {recordScope.value &&
                                        <div className="selected-data-container">
                                            <div className="selected-data">
                                                {recordScope.value.name}
                                            </div>
                                        </div>
                                        }
                                    </div>
                                }
                                {
                                    this.props.fund && transformationName.value && <div>
                                        <Input type="select" label={i18n('arr.fund.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                                            <option key='-ruleSetId'/>
                                            {ruleSets.map(i=> {
                                                return <option value={i.id}>{i.name}</option>
                                            })}
                                        </Input>
                                        <Input type="select" disabled={ruleSetId.invalid}
                                               label={i18n('arr.fund.arrType')} {...rulArrTypeId} {...decorateFormField(rulArrTypeId)}>
                                            <option key='-rulArrTypeId'/>
                                            {ruleSetOptions}
                                        </Input>
                                    </div>
                                }
                                <Input type="checkbox"
                                       label={i18n('import.stopOnError')} {...stopOnError} {...decorateFormField(stopOnError)} />

                                <label>{i18n('import.file')}</label>
                                <Input type="file" {...xmlFile} {...decorateFormField(xmlFile)} value={null}/>
                            </form>
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={handleSubmit(this.save)}>{i18n('global.action.import')}</Button>
                            <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                        </Modal.Footer>
                    </div>
                }
                {this.state.isRunning && <div>
                    <Modal.Body>
                        <Icon className="fa-spin" glyph="fa-refresh"/> {i18n('import.running')}
                    </Modal.Body>
                </div>}
            </div>
        )
    }
};

ImportForm.propTypes = {
    party: React.PropTypes.bool,
    record: React.PropTypes.bool,
    fund: React.PropTypes.bool
};


module.exports = reduxForm({
    form: 'importForm',
    fields: ['ruleSetId', 'rulArrTypeId', 'transformationName', 'recordScope', 'stopOnError', 'xmlFile'],
    validate
}, state => ({
    defaultScopes: state.defaultScopes,
    refTables: state.refTables
}))(ImportForm);
