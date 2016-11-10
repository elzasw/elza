import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, i18n, Autocomplete} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

//TODO @compel
class PartyCreatorForm extends AbstractReactComponent {
    static fields = [];
    render() {
        const {onClose} = this.props;

        return <Form onSubmit={this.handleSubmit}>
            <Modal.Body>
                <Autocomplete
                    label={i18n('party.creator.creator')}
                    customFilter
                    className='autocomplete-party'
                    items={this.state.partyList}
                    getItemId={(item) => item ? item.id : null}
                    getItemName={(item) => item ? item.name : ''}
                    onSearchChange={this.handleSearchChange}
                    onChange={this.updateValue}
                    renderItem={this.props.renderParty}
                    value={value} />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit">{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'PartyCreatorForm',
    fields: PartyCreatorForm.fields,
})(PartyCreatorForm)



