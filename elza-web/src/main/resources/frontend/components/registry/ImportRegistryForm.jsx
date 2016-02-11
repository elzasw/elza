/**
 * Formulář importu rejstříkových hesel
 * <ImportRegistryForm create onSubmit={this.handleCallImportRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils';
import {WebApi} from 'actions'

const validate = (values, props) => {
    const errors = {};
    if (!values.recordScopeId) {
        errors.recordScopeId = i18n('global.validation.required');
    }
    
    return errors;
};

var ImportRegistryForm = class ImportRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {defaultScopes: [], transformationNames: []};
        
        this.handleUpload = this.handleUpload.bind(this);
        
        WebApi.getDefaultScopes().then(json=>{
                this.state = {defaultScopes: json, transformationNames: this.state.transformationNames}
                this.render();
            });
        WebApi.getTransformations().then(json=>{
                this.state = {transformationNames: json, defaultScopes: this.state.defaultScopes}
                this.render();
            });;
    }

    componentWillReceiveProps(nextProps) {
    }

    handleUpload() {
        //console.log(this.props.fields.recordScopeId.value);
        //console.log(this.props.fields.transformationName.value);
        //console.log(this.props.fields.stopOnError.value);
        var file = this.refs.xmlFile;
        //console.log(file.refs.input.files[0]);
        
        var data = {
            transformationName: this.props.fields.transformationName.value,
            recordScopeId: this.props.fields.recordScopeId.value,
            stopOnError: this.props.fields.stopOnError.value,
            xmlMultipartFile: file.refs.input.files[0],
            importDataFormat: 'ELZA'
        };
        WebApi.importRegistry(data).then(json => {
            this.dispatch(modalDialogHide());
        });  
    }

    render() {
        const {fields: {transformationName, recordScopeId, stopOnError, xmlFile}, handleSubmit, onClose} = this.props;

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="select" label={i18n('registry.import.transformationName')} {...transformationName} {...decorateFormField(transformationName)}>
                            <option key=''></option>
                            {this.state.transformationNames.map(i=> {return <option value={i}>{i}</option>})}
                        </Input>
                        <Input type="select" label={i18n('registry.import.registryScope')} {...recordScopeId} {...decorateFormField(recordScopeId)} >
                            <option key=''></option>
                            {this.state.defaultScopes.map(i=> {return <option value={i.id}>{i.name}</option>})}
                        </Input>

                        <Input type="checkbox" label={i18n('registry.import.stopOnError')} {...stopOnError} {...decorateFormField(stopOnError)} />
                        
                        <Input ref="xmlFile" name="xmlFile" type="file" value={null} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleUpload}>{i18n('global.action.import')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'importRegistryForm',
    fields: ['transformationName', 'recordScopeId', 'stopOnError', 'xmlFile'],
    validate
},state => ({
    initialValues: state.form.addFaForm.initialValues,
    defaultScopes: state.defaultScopes,
}),
)(ImportRegistryForm)
