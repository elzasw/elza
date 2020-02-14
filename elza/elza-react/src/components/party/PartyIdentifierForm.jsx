/**
 * Formulář přidání nového / úpravu identifikátoru osobě
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Form, Modal, Button, Row, Col} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

import './RelationForm.scss'
import DatationField from "./DatationField";

class PartyIdentifierForm extends AbstractReactComponent {

    static fields = [
        'source',
        'note',
        'identifier',
        'toText',
        'from.value',
        'from.calendarTypeId',
        'from.textDate',
        'from.note',
        'to.value',
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

    submitReduxForm = (values, dispatch) => submitForm(PartyIdentifierForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {onClose, handleSubmit, fields:{source, note, identifier, from, to,}} = this.props;

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body className="dialog-3-col party-identifier-form">
                <div className="flex">
                    <div className="flex-2 col">
                        <Row>
                            <Col xs={12}>
                                <FormInput type="text" label={i18n('party.identifier.source')} {...source} />
                                <FormInput type="text" label={i18n('party.identifier.identifierText')} {...identifier} />
                            </Col>
                        </Row>
                    </div>
                    <div className="datation-group flex-1 col">
                        <Row>
                            <Col xs={12}>
                                <Row>
                                    <Col xs={6} md={12}>
                                        <DatationField fields={from} label={i18n('party.identifier.from')} labelTextual={i18n('party.identifier.from.textDate')} labelNote={i18n('party.identifier.from.note')} />
                                    </Col>
                                    <Col xs={6} md={12}>
                                        <DatationField fields={to} label={i18n('party.identifier.to')} labelTextual={i18n('party.identifier.to.textDate')} labelNote={i18n('party.identifier.to.note')} />
                                    </Col>
                                </Row>
                            </Col>
                        </Row>
                    </div>
                    <div className="flex-1 col">
                        <Row>
                            <Col xs={12}>
                                <FormInput as="textarea" label={i18n('party.identifier.note')} {...note} />
                            </Col>
                        </Row>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="default" type="submit">{i18n('global.action.store')}</Button>
                <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'partyIdentifierForm',
    fields: PartyIdentifierForm.fields,
})(PartyIdentifierForm)
