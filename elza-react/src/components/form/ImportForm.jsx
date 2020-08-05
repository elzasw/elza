import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {connect} from 'react-redux';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField} from 'components/form/FormUtils.jsx';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx';
import {WebApi} from 'actions/index.jsx';
import {importForm} from 'actions/global/global.jsx';
import AbstractReactComponent from '../AbstractReactComponent';
import FormInput from 'components/shared/form/FormInput';
import Autocomplete from '../shared/autocomplete/Autocomplete';
import i18n from '../i18n';
import Icon from '../shared/icon/Icon';
import FormInputField from '../shared/form/FormInputField';
import FF from '../shared/form/FF';
import FileInput from '../shared/form/FileInput';

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

    static propTypes = {
        record: PropTypes.bool,
        fund: PropTypes.bool,
    };

    state = {
        defaultScopes: [],
        transformationNames: [],
        isRunning: false,
    };

    componentDidMount() {
        this.props.dispatch(refRuleSetFetchIfNeeded());
        WebApi.getAllScopes().then(json => {
            this.setState({
                defaultScopes: json,
            });
        });
        WebApi.getTransformations().then(json => {
            this.setState({
                transformationNames: json,
            });
        });
    }

    save = values => {
        //validate
        this.setState({
            isRunning: true,
        });

        const data = {
            xmlFile: values.xmlFile,
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
        const messageType = this.props.fund ? 'Fund' : this.props.record ? 'Record' : 'Unknown';
        this.props.dispatch(importForm(formData, messageType));
    };

    render() {
        const {onClose, handleSubmit} = this.props;

        return (
            <div>
                {!this.state.isRunning && (
                    <div>
                        <Form onSubmit={handleSubmit(this.save)}>
                            <Modal.Body>
                                <div>
                                    <FF
                                        as="select"
                                        label={i18n('import.transformationName')}
                                        name={'transformationName'}
                                    >
                                        <option key="blankName" />
                                        {this.state.transformationNames.map((i, index) => {
                                            return (
                                                <option key={index + 'name'} value={i}>
                                                    {i}
                                                </option>
                                            );
                                        })}
                                    </FF>
                                    <FF
                                        field={Autocomplete}
                                        name={'recordScope'}
                                        //{...decorateFormField(recordScope)}
                                        help={null} /// TODO odstranit z decorateFormField help
                                        label={i18n('import.registryScope')}
                                        items={this.state.defaultScopes}
                                        getItemId={this.getItemId}
                                        getItemName={item => (item ? item.name : '')}
                                    />
                                    {/*recordScope.value && (
                                        <div className="selected-data-container">
                                            <div className="selected-data">{recordScope.value.name}</div>
                                        </div>
                                    )*/}
                                </div>

                                <FF field={FileInput} label={i18n('import.file')} type="file" name={'xmlFile'} />
                            </Modal.Body>
                            <Modal.Footer>
                                <Button variant="outline-secondary" type="submit" onClick={handleSubmit(this.save)}>
                                    {i18n('global.action.import')}
                                </Button>
                                <Button variant="link" onClick={onClose}>
                                    {i18n('global.action.cancel')}
                                </Button>
                            </Modal.Footer>
                        </Form>
                    </div>
                )}
                {this.state.isRunning && (
                    <div>
                        <Modal.Body>
                            <Icon className="fa-spin" glyph="fa-refresh" /> {i18n('import.running')}
                        </Modal.Body>
                    </div>
                )}
            </div>
        );
    }

    getItemId(item) {
        return item ? item.id : null;
    }
}

export default connect(state => ({
    defaultScopes: state.defaultScopes,
    refTables: state.refTables,
}))(
    reduxForm({
        form: 'importForm',
        validate: ImportForm.validate,
    })(ImportForm),
);
