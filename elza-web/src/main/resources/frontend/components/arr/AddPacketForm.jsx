/**
 * Formulář přidání obalu.
 */

require ('./AddPacketForm.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {indexById, getMapFromList} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (props.createSingle) {
        if (!values.storageNumber) {
            errors.storageNumber = i18n('global.validation.required');
        }
    } else if (props.createMany || props.changeNumbers) {
        if (!values.start) {
            errors.start = i18n('global.validation.required');
        }
        if (!values.count) {
            errors.count = i18n('global.validation.required');
        }
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

    zeroFill(number, width) {
        width -= number.toString().length;
        if ( width > 0 )
        {
            return new Array( width + (/\./.test( number ) ? 2 : 1) ).join( '0' ) + number;
        }
        return number + ""; // always return a string
    }

    render() {
        const {fields: {packetTypeId, storageNumber, prefix, start, size, count}, handleSubmit, onClose, packetTypes, createSingle, createMany, changeNumbers} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        var example
        if (createMany || changeNumbers) {
            if (prefix.value && start.value && size.value) {
                example = prefix.value + this.zeroFill(start.value, size.value)
            }
        }

        let packetTypeHelp;
        if (changeNumbers) {
            if (typeof packetTypeId.value === 'undefined' || packetTypeId.value === '') {
                packetTypeHelp = i18n('arr.packet.changeNumbers.packetType.empty')
            } else {
                const packetType = getMapFromList(packetTypes.items)[packetTypeId.value]
                packetTypeHelp = i18n('arr.packet.changeNumbers.packetType.notEmpty', packetType.name)
            }
        }

        return (
            <div className="add-packet-form-container">
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <FormInput componentClass="select" label={i18n('arr.packet.packetType')} {...packetTypeId} {...decorateFormField(packetTypeId)} help={packetTypeHelp}>
                            <option key='-packetTypeId'/>
                            {packetTypes.items.map(i=> {return <option key={i.id} value={i.id}>{i.name}</option>})}
                        </FormInput>
                        {createSingle && <FormInput type="text" label={i18n('arr.packet.storageNumber')} {...storageNumber} {...decorateFormField(storageNumber)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.prefix')} {...prefix} {...decorateFormField(prefix)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.start')} {...start} {...decorateFormField(start)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.size')} {...size} {...decorateFormField(size)} />}
                        {(createMany || changeNumbers) && <FormInput disabled={changeNumbers} type="text" label={i18n('arr.packet.count')} {...count} {...decorateFormField(count)} />}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <div className="packet-example">{example && i18n('arr.packet.example', example)}</div>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addPacketForm',
    fields: ['packetTypeId', 'storageNumber', 'prefix', 'start', 'size', 'count'],
    normalize: ['start', 'size', 'count']
},state => ({
    initialValues: state.form.addPacketForm.initialValues,
    packetTypes: state.refTables.packetTypes
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPacketForm', data})}
)(AddPacketForm)
