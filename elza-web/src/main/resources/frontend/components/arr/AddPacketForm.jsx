/**
 * Formulář přidání obalu.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.storageNumber) {
        errors.storageNumber = i18n('global.validation.required');
    }

    return errors;
};

var AddPacketForm = class AddPacketForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(packetsFetchIfNeeded(this.props.fundId));
        this.props.load(this.props.initData);
    }

    render() {
        const {fields: {packetTypeId, storageNumber, invalidPacket}, handleSubmit, onClose, packetTypes} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input type="select" label={i18n('arr.packet.packetType')} {...packetTypeId} {...decorateFormField(packetTypeId)}>
                            <option key='-packetTypeId'></option>
                            {packetTypes.items.map(i=> {return <option key={i.id} value={i.id}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('arr.packet.storageNumber')} {...storageNumber} {...decorateFormField(storageNumber)} />
                        <Input type="checkbox" label={i18n('arr.packet.invalidPacket')} {...invalidPacket} {...decorateFormField(invalidPacket)} value={true}  />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addPacketForm',
    fields: ['packetTypeId', 'storageNumber', 'invalidPacket'],
},state => ({
    initialValues: state.form.addPacketForm.initialValues,
    packetTypes: state.refTables.packetTypes
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPacketForm', data})}
)(AddPacketForm)



