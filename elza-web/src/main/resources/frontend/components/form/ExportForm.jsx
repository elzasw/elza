/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Autocomplete, Icon} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils';
import {WebApi} from 'actions'

var ExportForm = class ExportForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {defaultScopes: [], transformationNames: [], isFetching: true};
    }

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
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={submitForm}>
                        {isFetching ? <Loading /> : <Input type="select"
                                   label={i18n('export.transformationName')}
                                    {...transformationName}
                                    {...decorateFormField(transformationName)}
                            >
                                <option key='blankName'/>
                                {this.state.transformationNames.map((i, index)=> {
                                    return <option key={index+'name'} value={i}>{i}</option>
                                })}
                            </Input>
                        }
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={submitForm}>{i18n('global.action.export')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

ExportForm.propTypes = {};

module.exports = reduxForm({
    form: 'exportForm',
    fields: ['transformationName']
})(ExportForm);
