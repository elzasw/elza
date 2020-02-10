import React from 'react';
import {reduxForm} from 'redux-form';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils.jsx';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {WebApi} from 'actions/index.jsx';
import {importForm} from 'actions/global/global.jsx';
import AbstractReactComponent from "../AbstractReactComponent";
import FormInput from "../shared/form/FormInput";
import Autocomplete from "../shared/autocomplete/Autocomplete";
import i18n from "../i18n";
import Icon from "../shared/icon/Icon";

/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */
class ImportForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};
        
        if (!values.recordScope || !values.recordScope.id) {
            errors.recordScope = i18n('global.validation.required');
        }
        if (!values.xmlFile || values.xmlFile == null) {
            errors.xmlFile = i18n('global.validation.required');
        }
        return errors;
    };

    static PropTypes = {
        party: React.PropTypes.bool,
        record: React.PropTypes.bool,
        fund: React.PropTypes.bool
    };

    state = {
        defaultScopes: [],
        transformationNames: [],
        isRunning: false
    };

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
        WebApi.getAllScopes().then(json => {
            this.setState({
                defaultScopes: json
            });
        });
        WebApi.getTransformations().then(json => {
            this.setState({
                transformationNames: json
            });
        });
    }

    save = (values) => {
        //validate
        this.setState({
            isRunning: true,
        });

        const data = {
            xmlFile: values.xmlFile[0],
        };

        if (values.transformationName) {
            data.transformationName = values.transformationName;
        }

        if (values.recordScope && values.recordScope.id) {
            data.scopeId = values.recordScope.id;
        }

        const formData = new FormData();

        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        const messageType = this.props.fund ? 'Fund' : this.props.record ? 'Record' : 'Party';
        this.dispatch(importForm(formData, messageType));
    };

    render() {
        const {fields: {transformationName, recordScope, xmlFile}, onClose, handleSubmit, refTables, values} = this.props;

        return (
            <div>
                {
                    !this.state.isRunning && <div>
                        <Form onSubmit={handleSubmit(this.save)}>
                            <Modal.Body>
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
                                            label={i18n('import.registryScope')}
                                            items={this.state.defaultScopes}
                                            getItemId={(item) => item ? item : null}
                                            getItemName={(item) => item ? item.name : ''}
                                            onChange={
                                                value => {
                                                    if (value) {
                                                        recordScope.onChange({id: value.id, name: value.name});
                                                    }
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
                                
                                <label>{i18n('import.file')}</label>
                                <FormInput type="file" {...xmlFile} {...decorateFormField(xmlFile)} value={null}/>
                            </Modal.Body>
                            <Modal.Footer>
                                <Button type="submit" onClick={handleSubmit(this.save)}>{i18n('global.action.import')}</Button>
                                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                            </Modal.Footer>
                        </Form>
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
}

export default reduxForm({
    form: 'importForm',
    fields: ['ruleSetId', 'transformationName', 'recordScope', 'stopOnError', 'xmlFile'],
    validate: ImportForm.validate
}, state => ({
    defaultScopes: state.defaultScopes,
    refTables: state.refTables
}))(ImportForm);
