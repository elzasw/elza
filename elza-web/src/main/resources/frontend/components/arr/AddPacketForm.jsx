import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {indexById, getMapFromList} from 'stores/app/utils.jsx'
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import './AddPacketForm.less';

/**
 * Formulář přidání obalu.
 */
class AddPacketForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
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

    state = {};

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

    submitReduxForm = (values, dispatch) => submitForm(AddPacketForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {packetTypeId, storageNumber, prefix, start, size, count}, handleSubmit, onClose, packetTypes,
            createSingle, createMany, changeNumbers, arrRegion, submitting} = this.props;

        var activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }

        let packetTypeItems;

        if (activeFund == null) {
            packetTypeItems = packetTypes.items;
        } else {
            let activeVersion = activeFund.activeVersion;
            packetTypeItems = [];
            packetTypes.items.forEach(item => {
                if (activeVersion.packageId === item.packageId) {
                    packetTypeItems.push(item);
                }
            })
        }

        var example;
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
                const packetType = getMapFromList(packetTypeItems)[packetTypeId.value]
                packetTypeHelp = packetType && i18n('arr.packet.changeNumbers.packetType.notEmpty', packetType.name);
            }
        }

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <div className="add-packet-form-container">
                <Modal.Body>

                        <FormInput componentClass="select" label={i18n('arr.packet.packetType')} {...packetTypeId} {...decorateFormField(packetTypeId)} help={packetTypeHelp}>
                            <option key='-packetTypeId'/>
                            {packetTypeItems.map(i=> {return <option key={i.id} value={i.id}>{i.name}</option>})}
                        </FormInput>
                        {createSingle && <FormInput type="text" label={i18n('arr.packet.storageNumber')} {...storageNumber} {...decorateFormField(storageNumber)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.prefix')} {...prefix} {...decorateFormField(prefix)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.start')} {...start} {...decorateFormField(start)} />}
                        {(createMany || changeNumbers) && <FormInput type="text" label={i18n('arr.packet.size')} {...size} {...decorateFormField(size)} />}
                        {(createMany || changeNumbers) && <FormInput disabled={changeNumbers} type="text" label={i18n('arr.packet.count')} {...count} {...decorateFormField(count)} />}
                </Modal.Body>
                <Modal.Footer>
                    <div className="packet-example">{example && i18n('arr.packet.example', example)}</div>
                    <Button type="submit" disabled={submitting}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        </Form>
    }
}

export default reduxForm({
    form: 'addPacketForm',
    fields: ['packetTypeId', 'storageNumber', 'prefix', 'start', 'size', 'count'],
    normalize: ['start', 'size', 'count']
},state => ({
    initialValues: state.form.addPacketForm.initialValues,
    packetTypes: state.refTables.packetTypes,
    arrRegion: state.arrRegion
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPacketForm', data})}
)(AddPacketForm)
