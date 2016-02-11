/**
 * Formulář importu rejstříkových hesel
 * <ImportForm create onSubmit={this.handleCallImportRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Autocomplete, Icon} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils';
import {WebApi} from 'actions'
import {modalDialogHide} from 'actions/global/modalDialog';

const validate = (values, props) => {
    const errors = {};
    if (!values.recordScope) {
        errors.recordScope = i18n('global.validation.required');
    }
    if (values.xmlFile === null) {
        errors.xmlFile = i18n('global.validation.required');
    }

    return errors;
};

var ImportForm = class ImportForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {defaultScopes: [], transformationNames: [], isRunning: false};
        this.bindMethods('handleSubmit');
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        WebApi.getDefaultScopes().then(json => {
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

    handleSubmit(values) {
        console.log(values);
        this.setState({
            isRunning: true,
        });
        var data = Object.assign({}, values, {
            xmlFile: values.xmlFile[0],
            importDataFormat: this.props.fa ? "FINDING_AID" : this.props.record ? "RECORD" : "PARTY",
            stopOnError: values.stopOnError ? values.stopOnError : false,
        });
        var formData = new FormData();

        for (var key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        WebApi.xmlImport(formData).then(() => {
            this.dispatch(modalDialogHide());
        });
    }

    render() {
        const {fields: {transformationName, recordScope, stopOnError, xmlFile}, onClose, handleSubmit} = this.props;
        return (
            <div>
                {
                    !this.state.isRunning && <div>
                        <Modal.Body>
                            <form onSubmit={handleSubmit(this.handleSubmit)}>
                                {
                                    this.props.party || this.props.record && <div>
                                        <Input type="select"
                                               label={i18n('import.transformationName')} {...transformationName} {...decorateFormField(transformationName)}>
                                            <option key='blankName'/>
                                            {this.state.transformationNames.map((i, index)=> {
                                                return <option key={index+'name'} value={i}>{i}</option>
                                            })}
                                        </Input>
                                        <Autocomplete
                                            {...recordScope} {...decorateFormField(recordScope)}
                                            label={i18n('import.registryScope')}// / Třída rejstříku
                                            items={this.state.defaultScopes}
                                            getItemId={(item) => item ? item : null}
                                            getItemName={(item) => item ? item.name : ''}
                                        />
                                    </div>
                                }
                                {
                                    this.props.fa && <div>
                                        <Autocomplete
                                            tags // TODO migrate autocomplete
                                            label={i18n('import.registryScope')}// / Třída rejstříku
                                            items={this.state.defaultScopes}
                                            getItemId={(item) => item ? item.id : null}
                                            getItemName={(item) => item ? item.name : ''}
                                        />
                                    </div>
                                }
                                <Input type="checkbox"
                                       label={i18n('import.stopOnError')} {...stopOnError} {...decorateFormField(stopOnError)} />

                                <label>{i18n('import.file')}</label>
                                <input type="file" {...xmlFile} value={null}/>
                            </form>
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={handleSubmit(this.handleSubmit)}>{i18n('global.action.import')}</Button>
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
    fa: React.PropTypes.bool
};


module.exports = reduxForm({
    form: 'importForm',
    fields: ['transformationName', 'recordScope', 'stopOnError', 'xmlFile'],
    validate
}, state => ({
    initialValues: state.form.addFaForm.initialValues,
    defaultScopes: state.defaultScopes
}))(ImportForm);
