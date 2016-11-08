import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Form, Modal, Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyListFetchIfNeeded} from 'actions/refTables/partyList.jsx'

/**
 * Formulář přidání nového / úpravu identifikátoru osobě
 */
class PartyIdentifierForm extends AbstractReactComponent {

    componentDidMount() {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
    }

    componentWillReceiveProps() {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
    }

    static fields = [
        'source',
        'note',
        'identifier',
        'fromText',
        'toText',
        'fromCalendar',
        'toCalendar',
    ];

    static validate = (values) => {
        let errors = {};

        if(!values.identifier) {
            errors['identifier'] = i18n('party.identifier.errors.undefinedIdentifierText');
        }

        return errors;
    };

    render() {
        const {
            refTables: {calendarTypes},
            onClose,
            handleSubmit,
            fields:{
                source,
                note,
                identifier,
                fromText,
                toText,
                fromCalendar,
                toCalendar,
            }
        } = this.props;

        const submit = submitReduxForm.bind(this, PartyIdentifierForm.validate);

        return <Form onSubmit={handleSubmit(submit)}>
            <Modal.Body>
                <FormInput type="text" label={i18n('party.identifier.source')} {...source} />
                <FormInput type="text" label={i18n('party.identifier.identifierText')} {...identifier} />
                <hr/>
                <div className="line datation">
                    <div className="date line">
                        <div>
                            <label>{i18n('party.identifier.from')}</label>
                            <div className="line">
                                <FormInput componentClass="select" {...fromCalendar}>
                                    {calendarTypes.items.map(i=> <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>)}
                                </FormInput>
                                <FormInput type="text" {...fromText} />
                            </div>
                        </div>
                        <div>
                            <label>{i18n('party.identifier.to')}</label>
                            <div className="line">
                                <FormInput componentClass="select" {...toCalendar}>
                                    {calendarTypes.items.map(i=> <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>)}
                                </FormInput>
                                <FormInput type="text" {...toText} />
                            </div>
                        </div>
                    </div>
                </div>
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
}, state => ({
    refTables: state.refTables
})
)(PartyIdentifierForm)



