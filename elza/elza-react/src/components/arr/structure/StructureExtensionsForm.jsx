import React from 'react';
import {Field, FieldArray, reduxForm} from 'redux-form';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { arrPanelMessages } from '../panelMessages';
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
                    <h5>{<FormattedMessage {...arrPanelMessages.structureModalSettingsExtensions} />}</h5>
                    <div className="listbox-wrapper">
                        <div className="listbox-container">
                            <FieldArray
                                name={'extensions'}
                                component={({fields, meta}) => {
                                    if (fields.length === 0) {
                                        return this.props.intl.formatMessage(arrPanelMessages.structureModalSettingsNoResults);
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
                        {<FormattedMessage {...globalMessages.save} />}
                    </Button>
                    <Button variant="link" disabled={submitting} onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'structureExtensions',
})(injectIntl(StructureExtensionsForm));
