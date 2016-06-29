/**
 * Formulář přidání výstupu.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.name) {
        errors.name = i18n('global.validation.required');
    }
    if (props.create && !values.outputTypeId) {
        errors.outputTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddOutputForm = class AddOutputForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(outputTypesFetchIfNeeded());
    }

    componentDidMount() {
        this.dispatch(outputTypesFetchIfNeeded());
        // this.props.load(this.props.initData);
    }

    render() {
        const {fields: {name, internalCode, temporary, templateId, outputTypeId}, create, handleSubmit, onClose, outputTypes, templates} = this.props;
        var submitForm = submitReduxForm.bind(this, validate)

        console.log(111, templateId)

        return (
            <div className="add-output-form-container">
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input type="text" label={i18n('arr.output.name')} {...name} {...decorateFormField(name)} />
                        <Input type="text" label={i18n('arr.output.internalCode')} {...internalCode} {...decorateFormField(internalCode)} />
                        {create && <Input type="select" label={i18n('arr.output.outputType')} {...outputTypeId} {...decorateFormField(outputTypeId)}>
                            <option key='-outputTypeId'/>
                            {outputTypes.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </Input>}
                        <Input type="select" label={i18n('arr.output.template')} {...templateId} {...decorateFormField(templateId)}>
                            <option key='-templateId'/>
                            {templates.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </Input>
                        {create && <Input type="checkbox" label={i18n('arr.output.temporary')} {...temporary} {...decorateFormField(temporary)} />}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{create ? i18n('global.action.create') : i18n('global.action.update')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

AddOutputForm.propTypes = {
    create: React.PropTypes.bool,
    initData: React.PropTypes.object,
    onSubmitForm: React.PropTypes.func.isRequired,
    templates: React.PropTypes.array.isRequired
};

module.exports = reduxForm({
        form: 'addOutputForm',
        fields: ['name', 'internalCode', 'temporary', 'outputTypeId', "templateId"],
    },(state, props) => {
        return {
            initialValues: props.create ? {temporary: false} : props.initData,
            outputTypes: state.refTables.outputTypes.items,
            templates: state.refTables.templates.items,
        }
    },
    {/*load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPacketForm', data})*/}
)(AddOutputForm);
