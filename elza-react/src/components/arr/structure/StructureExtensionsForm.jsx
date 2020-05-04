import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, FormCheck, Modal} from 'react-bootstrap';
import {Button} from '../../ui';

import './StructureExtensionsForm.scss';

class StructureExtensionsForm extends AbstractReactComponent {
    render() {
        const {
            fields: {extensions},
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
                            {extensions && extensions.length > 0
                                ? extensions.map((val, index) => {
                                      const {checked, name, onFocus, onChange, onBlur} = val.active;
                                      const wantedProps = {checked, name, onFocus, onChange, onBlur};
                                      return (
                                          <FormCheck {...wantedProps} key={index} value={true} label={val.name.initialValue} />
                                      );
                                  })
                                : i18n('arr.structure.modal.settings.noResults')}
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
    fields: ['extensions[].code', 'extensions[].name', 'extensions[].active'],
})(StructureExtensionsForm);
