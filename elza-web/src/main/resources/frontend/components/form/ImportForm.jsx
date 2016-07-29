/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Autocomplete, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button, Checkbox} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils.jsx';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {WebApi} from 'actions/index.jsx';
import {importForm} from 'actions/global/global.jsx';

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

const ImportForm = class ImportForm extends AbstractReactComponent {

    static PropTypes = {
        party: React.PropTypes.bool,
        record: React.PropTypes.bool,
        fund: React.PropTypes.bool
    };

    constructor(props) {
        super(props);
        this.state = {defaultScopes: [], transformationNames: [], isRunning: false};
        this.bindMethods('save');
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

        const data = {
            xmlFile: values.xmlFile[0],
            importDataFormat: this.props.fund ? 'FUND' : (this.props.record ? 'RECORD' : 'PARTY'),
            stopOnError: values.stopOnError && values.stopOnError == 1 ? values.stopOnError : false
        };

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

        const formData = new FormData();

        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        const messageType = this.props.fund ? 'Fund' : this.props.record ? 'Record' : 'Party';
        this.dispatch(importForm(formData, messageType));
    }

    render() {
        const {fields: {ruleSetId, rulArrTypeId, transformationName, recordScope, stopOnError, xmlFile}, onClose, handleSubmit, refTables, values} = this.props;
        const ruleSets = refTables.ruleSet.items;
        const currRuleSetId = values.ruleSetId;
        let currRuleSet = [];
        let ruleSetOptions = [];
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
            if (currRuleSet) {
                ruleSetOptions = currRuleSet.arrangementTypes.map(
                    i => <option key={i.id} value={i.id}>{i.name}</option>
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
                                        <FormInput componentClass="select" label={i18n('import.transformationName')} {...transformationName}>
                                            <option key='blankName'/>
                                            {this.state.transformationNames.map((i, index)=> {
                                                return <option key={index+'name'} value={i}>{i}</option>
                                            })}
                                        </FormInput>
                                        <Autocomplete
                                            {...recordScope}
                                            {...decorateFormField(recordScope)}
                                            help={null} /// TODO odstranit z decorateFormField help
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
                                        <FormInput componentClass="select" label={i18n('arr.fund.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                                            <option key='-ruleSetId'/>
                                            {ruleSets.map(i=> {
                                                return <option value={i.id}>{i.name}</option>
                                            })}
                                        </FormInput>
                                        <FormInput componentClass="select" disabled={ruleSetId.invalid}
                                               label={i18n('arr.fund.arrType')} {...rulArrTypeId} {...decorateFormField(rulArrTypeId)}>
                                            <option key='-rulArrTypeId'/>
                                            {ruleSetOptions}
                                        </FormInput>
                                    </div>
                                }
                                <Checkbox{...stopOnError} {...decorateFormField(stopOnError)}>{i18n('import.stopOnError')}</Checkbox>

                                <label>{i18n('import.file')}</label>
                                <FormInput type="file" {...xmlFile} {...decorateFormField(xmlFile)} value={null}/>
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

module.exports = reduxForm({
    form: 'importForm',
    fields: ['ruleSetId', 'rulArrTypeId', 'transformationName', 'recordScope', 'stopOnError', 'xmlFile'],
    validate
}, state => ({
    defaultScopes: state.defaultScopes,
    refTables: state.refTables
}))(ImportForm);
