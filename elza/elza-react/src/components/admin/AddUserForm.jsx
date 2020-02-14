/**
 * Formulář přidání nebo uzavření AS.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Button, FormCheck, Col, Form, Modal, Row} from 'react-bootstrap';
import {submitForm} from 'components/form/FormUtils.jsx'
import PartyField from "../party/PartyField";
import './AddUserForm.scss';

class AddUserForm extends AbstractReactComponent {

    static defaultProps = {
        create: false
    };

    static propTypes = {
        onCreateParty: PropTypes.func,
        create: PropTypes.bool
    };

    constructor(props) {
        super(props);

        this.state = {
            createParty: false,
            setPassword: !props.values.passwordCheckbox,
            setShibboleth: !props.values.shibbolethCheckbox,
        };
    }

    static validate(state, values, props) {
        const {create} = props;
        const {setPassword, setShibboleth} = this.state;
        const errors = {};

        let fields = ['username'];

        if (props.create) {
            fields.push('party');
        }

        for (let field of fields) {
            if (!values[field]) {
                errors[field] = i18n('global.validation.required');
            }
        }

        if (values.passwordCheckbox && values.password !== values.passwordAgain) {
            errors.password = i18n('admin.user.validation.passNotEqual');
            errors.passwordAgain = i18n('admin.user.validation.passNotEqual');
        } else {
            if (values.passwordCheckbox && !values.password && (create || setPassword)) {
                errors.password = i18n('global.validation.required');
            }
            if (values.passwordCheckbox && !values.passwordAgain && (create || setPassword)) {
                errors.passwordAgain = i18n('global.validation.required');
            }
        }

        if (values.shibbolethCheckbox && !values.shibboleth && (create || setShibboleth)) {
            errors.shibboleth = i18n('global.validation.required');
        }

        return errors;
    }

    handlePartyCreate = (partyTypeId) => {
        const {onCreateParty} = this.props;

        onCreateParty && onCreateParty(partyTypeId, this.handlePartyReceive);
    };

    handlePartyReceive = (newParty) => {
        this.props.fields.party.onChange(newParty);
    };

    transformData = (data) => {
        const {create} = this.props;
        const {passwordAgain, shibbolethCheckbox, password, shibboleth, passwordCheckbox, ...other} = data;

        let newData = {
            ...other,
            valuesMap: {}
        };

        if (passwordCheckbox) {
            newData.valuesMap['PASSWORD'] = password && password.length > 0 ? password : null;
        }

        if (shibbolethCheckbox) {
            newData.valuesMap['SHIBBOLETH'] = shibboleth && shibboleth.length > 0 ? shibboleth : null;
        }

        return this.props.onSubmitForm(newData);
    };

    submitReduxForm = (values, dispatch) => submitForm(AddUserForm.validate.bind(this, this.state),values,this.props,this.transformData,dispatch);

    render() {
        const {fields: {username, password, passwordAgain, passwordCheckbox, shibbolethCheckbox, party, shibboleth}, create, handleSubmit, onClose, submitting} = this.props;
        const {setPassword, setShibboleth} = this.state;
        return <Form className="add-user-form" onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    {create && <Row>
                        <Col xs={12}><PartyField disabled={submitting} label={i18n('admin.user.add.party')} {...party}  onCreate={this.handlePartyCreate} detail={false} /></Col>
                    </Row>}
                    <Row>
                        <Col xs={12}><FormInput disabled={submitting} label={i18n('admin.user.add.username')} autoComplete="off" type="text" {...username} /></Col>
                    </Row>
                    <Row className="type-row-group">
                        <Col xs={12}><span className="type">Způsob přihlášení</span></Col>
                    </Row>
                    <FormCheck disabled={submitting} label={i18n('admin.user.add.password.checkbox')} type="checkbox" {...passwordCheckbox}>{i18n('admin.user.add.password.checkbox')}</FormCheck>
                    {passwordCheckbox.value && (create || setPassword) && <Row className="type-row">
                        <Col xs={6}><FormInput disabled={submitting} label={i18n(create ? 'admin.user.password' : 'admin.user.newPassword' )} autoComplete="off" type="password" {...password} /></Col>
                        <Col xs={6}><FormInput disabled={submitting} label={i18n('admin.user.passwordAgain')} autoComplete="off" type="password" {...passwordAgain} /></Col>
                    </Row>}
                    {passwordCheckbox.value && !create && !setPassword && <Row className="type-row">
                        <Col xs={6} className="message">{i18n('admin.user.add.password.message')}</Col>
                        <Col xs={6}><Button disabled={submitting} onClick={() => this.setState({setPassword: true})}>{i18n('global.action.change')}</Button></Col>
                    </Row>}
                    <FormCheck disabled={submitting} label={i18n('admin.user.add.shibboleth.checkbox')} type="checkbox" {...shibbolethCheckbox}>{i18n('admin.user.add.shibboleth.checkbox')}</FormCheck>
                    {shibbolethCheckbox.value && (create || setShibboleth) && <Row className="type-row">
                        <Col xs={12}><FormInput disabled={submitting} label={i18n('admin.user.add.shibboleth')} autoComplete="off" type="text" {...shibboleth} /></Col>
                    </Row>}
                    {shibbolethCheckbox.value && !create && !setShibboleth && <Row className="type-row">
                        <Col xs={6} className="message">{i18n('admin.user.add.shibboleth.message')}</Col>
                        <Col xs={6}><Button disabled={submitting} onClick={() => this.setState({setShibboleth: true})}>{i18n('global.action.change')}</Button></Col>
                    </Row>}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" disabled={submitting}>{i18n(create ? 'global.action.create' : 'global.action.update')}</Button>
                    <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'addUserForm',
    fields: ['username', 'password', 'passwordAgain', 'party', 'passwordCheckbox', 'shibbolethCheckbox', 'shibboleth'],
})(AddUserForm);



