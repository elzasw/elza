import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput, DatationField} from 'components/index.jsx';
import {Form, Modal, Button, Row, Col} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

import './PartyIdentifierForm.less'

/**
 * Formulář přidání nového / úpravu identifikátoru osobě
 */
class PartyIdentifierForm extends AbstractReactComponent {

    static fields = [
        'source',
        'note',
        'identifier',
        'toText',
        'from.valueFrom',
        'from.calendarTypeId',
        'from.textDate',
        'from.note',
        'to.valueFrom',
        'to.calendarTypeId',
        'to.textDate',
        'to.note',
    ];

    static validate = (values) => {
        let errors = {};

        if(!values.identifier) {
            errors['identifier'] = i18n('party.identifier.errors.undefinedIdentifierText');
        }

        return errors;
    };

    render() {
        const {onClose, handleSubmit, fields:{source, note, identifier, from, to,}} = this.props;

        const submit = submitReduxForm.bind(this, PartyIdentifierForm.validate);

        return <Form onSubmit={handleSubmit(submit)}>
            <Modal.Body className="party-identifier-form">
                <FormInput type="text" label={i18n('party.identifier.source')} {...source} />
                <FormInput type="text" label={i18n('party.identifier.identifierText')} {...identifier} />
                <hr/>
                <Row className="datations">
                    <Col xs={12} md={6}>
                        <DatationField fields={from} label={i18n('party.identifier.from')} labelTextual={i18n('party.identifier.from.textDate')} labelNote={i18n('party.identifier.from.note')} />
                    </Col>
                    <Col xs={12} md={6}>
                        <DatationField fields={to} label={i18n('party.identifier.to')} labelTextual={i18n('party.identifier.to.textDate')} labelNote={i18n('party.identifier.to.note')} />
                    </Col>
                </Row>
                <hr/>
                <FormInput type="text" label={i18n('party.identifier.note')} {...note} />
            </Modal.Body>
            <Modal.Footer>
                <Button bsStyle="default" type="submit">{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'partyIdentifierForm',
    fields: PartyIdentifierForm.fields,
})(PartyIdentifierForm)



