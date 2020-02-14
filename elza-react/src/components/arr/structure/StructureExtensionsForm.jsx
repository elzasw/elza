import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';

import './StructureExtensionsForm.scss'

class StructureExtensionsForm extends AbstractReactComponent {

    render() {
        const {fields: {extensions}, handleSubmit, onClose, submitting} = this.props;

        return (
            <Form className="structure-extensions-form" onSubmit={handleSubmit}>
                <Modal.Body>
                    <h5>{i18n('arr.structure.modal.settings.extensions')}</h5>
                    <div className="listbox-wrapper">
                        <div className="listbox-container">
                            {extensions && extensions.length > 0 ? extensions.map((val, index) => {
                                const {checked, name, onFocus, onChange, onBlur} = val.active;
                                const wantedProps = {checked, name, onFocus, onChange, onBlur};
                                return <Checkbox {...wantedProps} key={index} value={true}>
                                    {val.name.initialValue}
                                </Checkbox>
                            }) : i18n('arr.structure.modal.settings.noResults')}
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" disabled={submitting}>{i18n('global.action.update')}</Button>
                    <Button bsStyle="link" disabled={submitting} onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
        )
    }
}

export default reduxForm({
    form: 'structureExtensions',
    fields: ['extensions[].code', 'extensions[].name', 'extensions[].active']
})(StructureExtensionsForm);
