import React from 'react';
import {Field, FieldArray, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../../ui';

import './StructureExtensionsForm.scss';
import FormInputField from "../../shared/form/FormInputField";

class StructureExtensionsForm extends AbstractReactComponent {
    render() {
        const {
            handleSubmit,
            onClose,
            submitting,
        } = this.props;
        return (
            <Form className="structure-extensions-form" onSubmit={handleSubmit}>
                <Modal.Body>
                    <h5>{i18n('arr.structure.modal.settings.extensions')}</h5>
                    <div className="listbox-wrapper">
                        <div className="listbox-container">
                            <FieldArray
                                name={'extensions'}
                                component={({fields, meta}) => {
                                    if (fields.length === 0) {
                                        return i18n('arr.structure.modal.settings.noResults');
                                    }
                                    return fields.map((item, index, fields) => {
                                        return (
                                            <div key={index}>
                                                <Field
                                                    type="checkbox"
                                                    name={`${item}.active`}
                                                    component={FormInputField}
                                                    label={fields.get(index).name}
                                                    value={true}
                                                />
                                            </div>
                                        );
                                    });
                                }}
                            />
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.update')}
                    </Button>
                    <Button variant="link" disabled={submitting} onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'structureExtensions',
})(StructureExtensionsForm);
