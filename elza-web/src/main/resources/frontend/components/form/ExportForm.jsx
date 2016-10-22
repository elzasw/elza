import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Autocomplete, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx';
import {WebApi} from 'actions/index.jsx';

/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */
class ExportForm extends AbstractReactComponent {

    static PropTypes = {};

    state = {
        defaultScopes: [],
        transformationNames: [],
        isFetching: true
    };

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        WebApi.getExportTransformations().then(json => {
            this.setState({
                transformationNames: json,
                isFetching: false
            });
        });
    }

    render() {
        const {fields: {transformationName}, onClose, handleSubmit, isFetching} = this.props;
        const submitForm = handleSubmit(submitReduxForm.bind(this, () => ({})));
        return <Form onSubmit={submitForm}>
            <Modal.Body>
                {isFetching ? <Loading /> : <FormInput componentClass="select"
                           label={i18n('export.transformationName')}
                            {...transformationName}
                            {...decorateFormField(transformationName)}
                    >
                        <option key='blankName'/>
                        {this.state.transformationNames.map((i, index)=> {
                            return <option key={index+'name'} value={i}>{i}</option>
                        })}
                    </FormInput>
                }
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" onClick={submitForm}>{i18n('global.action.export')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'exportForm',
    fields: ['transformationName']
})(ExportForm);
