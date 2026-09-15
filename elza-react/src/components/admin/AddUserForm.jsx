/**
 * Formulář přidání nebo uzavření AS.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {Field, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInputField} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang';
import { getIntl } from 'components/shared/lang/intlInstance';

// Id jsou převzatá z legacy katalogu beze změny; nové id dostal jen nadpis
// "Způsob přihlášení", který byl v JSX natvrdo česky.
const messages = defineMessages({
    passNotEqual: { id: 'admin.user.validation.passNotEqual', defaultMessage: 'Zadaná hesla nejsou stejná' },
    party: { id: 'admin.user.add.party', defaultMessage: 'Osoba' },
    username: { id: 'admin.user.add.username', defaultMessage: 'Uživatelské jméno' },
    loginMethod: { id: 'admin.user.add.loginMethod', defaultMessage: 'Způsob přihlášení' },
    passwordCheckbox: { id: 'admin.user.add.password.checkbox', defaultMessage: 'Jméno a heslo' },
    password: { id: 'admin.user.password', defaultMessage: 'Heslo' },
    newPassword: { id: 'admin.user.newPassword', defaultMessage: 'Nové heslo' },
    passwordAgain: { id: 'admin.user.passwordAgain', defaultMessage: 'Opakovat heslo' },
    passwordMessage: { id: 'admin.user.add.password.message', defaultMessage: 'Heslo je nastaveno.' },
    shibbolethCheckbox: { id: 'admin.user.add.shibboleth.checkbox', defaultMessage: 'SAML2' },
    shibboleth: { id: 'admin.user.add.shibboleth', defaultMessage: 'Ověřovací token' },
    shibbolethMessage: { id: 'admin.user.add.shibboleth.message', defaultMessage: 'Ověřovací token je nastaven.' },
});
import {Col, Form, Modal, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import './AddUserForm.scss';
import {storeFromArea} from 'shared/utils';
import {AREA_EXT_SYSTEM_DETAIL} from 'actions/admin/extSystem';
import {connect} from 'react-redux';
import ApField from '../registry/ApField';
import ReduxFormFieldErrorDecorator from '../shared/form/ReduxFormFieldErrorDecorator';

class AddUserForm extends AbstractReactComponent {
    static defaultProps = {
        create: false,
    };

    static propTypes = {
        create: PropTypes.bool,
    };

    constructor(props) {
        super(props);

        this.state = {
            createParty: false,
            setPassword: !props.passwordCheckbox,
            setShibboleth: !props.shibbolethCheckbox,
        };
    }

    static validate(state, values, props) {
        const {create} = props;
        const {setPassword, setShibboleth} = this.state;
        const errors = {};

        let fields = ['username'];

        for (let field of fields) {
            if (!values[field]) {
                errors[field] = getIntl().formatMessage(globalMessages.validationRequired);
            }
        }

        if (values.passwordCheckbox && values.password !== values.passwordAgain) {
            errors.password = getIntl().formatMessage(messages.passNotEqual);
            errors.passwordAgain = getIntl().formatMessage(messages.passNotEqual);
        } else {
            if (values.passwordCheckbox && !values.password && (create || setPassword)) {
                errors.password = getIntl().formatMessage(globalMessages.validationRequired);
            }
            if (values.passwordCheckbox && !values.passwordAgain && (create || setPassword)) {
                errors.passwordAgain = getIntl().formatMessage(globalMessages.validationRequired);
            }
        }

        if (values.shibbolethCheckbox && !values.shibboleth && (create || setShibboleth)) {
            errors.shibboleth = getIntl().formatMessage(globalMessages.validationRequired);
        }

        return errors;
    }

    transformData = data => {
        const {passwordAgain, shibbolethCheckbox, password, shibboleth, passwordCheckbox, ...other} = data;

        let newData = {
            ...other,
            valuesMap: {},
        };

        if (passwordCheckbox) {
            newData.valuesMap['PASSWORD'] = password && password.length > 0 ? password : null;
        }

        if (shibbolethCheckbox) {
            newData.valuesMap['SHIBBOLETH'] = shibboleth && shibboleth.length > 0 ? shibboleth : null;
        }

        return this.props.onSubmitForm(newData);
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(AddUserForm.validate.bind(this, this.state), values, this.props, this.transformData, dispatch);

    render() {
        const {create, handleSubmit, onClose, submitting} = this.props;
        const {setPassword, setShibboleth} = this.state;
        return (
            <Form className="add-user-form" onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Row>
                        <Col xs={12}>
                            <Field
                                component={ReduxFormFieldErrorDecorator}
                                passOnly={true}
                                renderComponent={ApField}
                                disabled={submitting}
                                label={this.props.intl.formatMessage(messages.party)}
                                detail={false}
                                name={"accessPointId"}
                                initData={this.props.accessPoint ? [this.props.accessPoint] : null}
                                useIdAsValue
                                isCreate
                            />
                        </Col>
                    </Row>
                    <Row>
                        <Col xs={12}>
                            <Field
                                name="username"
                                type="text"
                                component={FormInputField}
                                label={this.props.intl.formatMessage(messages.username)}
                                disabled={submitting}
                            />
                        </Col>
                    </Row>
                    <Row className="type-row-group">
                        <Col xs={12}>
                            <span className="type"><FormattedMessage {...messages.loginMethod} /></span>
                        </Col>
                    </Row>
                    <Field
                        name="passwordCheckbox"
                        type="checkbox"
                        component={FormInputField}
                        label={this.props.intl.formatMessage(messages.passwordCheckbox)}
                        disabled={submitting}
                    />
                    {this.props.passwordCheckbox && (create || setPassword) && (
                        <Row className="type-row">
                            <Col xs={6}>
                                <Field
                                    name="password"
                                    type="password"
                                    component={FormInputField}
                                    label={this.props.intl.formatMessage(create ? messages.password : messages.newPassword)}
                                    disabled={submitting}
                                    autoComplete="off"
                                />
                            </Col>
                            <Col xs={6}>
                                <Field
                                    name="passwordAgain"
                                    type="password"
                                    component={FormInputField}
                                    label={this.props.intl.formatMessage(messages.passwordAgain)}
                                    disabled={submitting}
                                    autoComplete="off"
                                />
                            </Col>
                        </Row>
                    )}
                    {this.props.passwordCheckbox && !create && !setPassword && (
                        <Row className="type-row">
                            <Col xs={6} className="message">
                                <FormattedMessage {...messages.passwordMessage} />
                            </Col>
                            <Col xs={6}>
                                <Button
                                    disabled={submitting}
                                    variant="outline-secondary"
                                    onClick={() => this.setState({setPassword: true})}
                                >
                                    <FormattedMessage {...globalMessages.change} />
                                </Button>
                            </Col>
                        </Row>
                    )}
                    <Field
                        name="shibbolethCheckbox"
                        type="checkbox"
                        component={FormInputField}
                        label={this.props.intl.formatMessage(messages.shibbolethCheckbox)}
                        disabled={submitting}
                    />
                    {this.props.shibbolethCheckbox && (create || setShibboleth) && (
                        <Row className="type-row">
                            <Col xs={12}>
                                <Field
                                    name="shibboleth"
                                    type="text"
                                    component={FormInputField}
                                    label={this.props.intl.formatMessage(messages.shibboleth)}
                                    disabled={submitting}
                                    autoComplete="off"
                                />
                            </Col>
                        </Row>
                    )}
                    {this.props.shibbolethCheckbox && !create && !setShibboleth && (
                        <Row className="type-row">
                            <Col xs={6} className="message">
                                <FormattedMessage {...messages.shibbolethMessage} />
                            </Col>
                            <Col xs={6}>
                                <Button
                                    disabled={submitting}
                                    variant="outline-secondary"
                                    onClick={() => this.setState({setShibboleth: true})}
                                >
                                    <FormattedMessage {...globalMessages.change} />
                                </Button>
                            </Col>
                        </Row>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        <FormattedMessage {...(create ? globalMessages.create : globalMessages.save)} />
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        <FormattedMessage {...globalMessages.cancel} />
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const selector = formValueSelector('addUserForm');

function mapState(state) {
    const {splitter} = state;
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);

    return {
        passwordCheckbox: selector(state, 'passwordCheckbox'),
        shibbolethCheckbox: selector(state, 'shibbolethCheckbox'),
        splitter,
        extSystemDetail,
    };
}

const connector = connect(mapState);

export default reduxForm({
    form: 'addUserForm',
})(connector(injectIntl(AddUserForm)));
