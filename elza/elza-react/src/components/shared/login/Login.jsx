import React from 'react';
import {connect} from 'react-redux';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {checkUserLogged, login} from 'actions/global/login.jsx';
import {WebApi} from 'actions/index.jsx';

import './Login.scss';
import {ModalDialogWrapper} from '../dialog/ModalDialogWrapper';
import FormInput from 'components/shared/form/FormInput';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    errorUnknown: { id: 'login.error.unknown', defaultMessage: 'Neznámá chyba přihlášení' },
    formTitle: { id: 'login.form.title', defaultMessage: 'Přihlášení uživatele' },
    ssoKerberos: {
        id: 'login.action.ssoKerberos',
        defaultMessage: 'Přihlásit se pomocí Windows autentizace (Kerberos)',
    },
    defaultUserEnabled: {
        id: 'login.defaultUserEnabled',
        defaultMessage:
            'Je povolen výchozí uživatel. Vytvořte si vlastního uživatele s oprávněním administrátora a výchozího uživatele vypněte.',
    },
    username: { id: 'login.field.username', defaultMessage: 'Uživatelské jméno' },
    password: { id: 'login.field.password', defaultMessage: 'Heslo' },
    login: { id: 'login.action.login', defaultMessage: 'Přihlásit' },
});
import AbstractReactComponent from '../../AbstractReactComponent';

const defaultEnabled = typeof window.defaultUserEnabled !== 'undefined' && window.defaultUserEnabled;

const getDefaultLogin = () => {
    if (defaultEnabled) {
        return {
            username: 'admin',
            password: 'admin',
        };
    } else {
        return {
            username: '',
            password: '',
        };
    }
};

class Login extends AbstractReactComponent {
    defaultState = {
        ...getDefaultLogin(),
        error: null,
    };

    UNSAFE_componentWillMount() {
        this.props.dispatch(checkUserLogged());
    }

    state = {
        ...this.defaultState,
    };

    handleChange = (field, event) => {
        this.setState({[field]: event.target.value});
    };

    handleLoginError = err => {
        console.log(err);
        if (err.data && err.data.message) {
            this.setState({error: err.data.message});
        } else {
            this.setState({error: this.props.intl.formatMessage(messages.errorUnknown)});
        }
    };

    handleLogin = e => {
        e.preventDefault();
        const {username, password} = this.state;

        this.props
            .dispatch(login(username, password))
            .then(data => {
                this.setState({...this.defaultState});
            })
            .catch(err => {
                this.handleLoginError(err);
            });
    };

    render() {
        const {login, submitting, userDetail} = this.props;
        const {error, username, password} = this.state;

        // Login dialog is shown only when user is not logged in and data about user have been fetched
        // to prevent flicker on page reload
        const displayLoginDialog = !login.logged && userDetail.fetched;
        const displaySsoKerberos = typeof window.ssoKerberosUrl !== 'undefined' && window.ssoKerberosUrl;

        return (
            <div className="login-container">
                {displayLoginDialog && (
                    <ModalDialogWrapper className="login" title={this.props.intl.formatMessage(messages.formTitle)}>
                    {displaySsoKerberos && (
                            <div className="sso-kerberos-login">
                                <Button
                                    variant="outline-secondary"
                                    label={this.props.intl.formatMessage(messages.ssoKerberos)}
                                    onClick={() => {
                                        window.location.href = window.serverContextPath +window.ssoKerberosUrl;
                                    }}
                                >
                                    <FormattedMessage {...messages.ssoKerberos} />
                                </Button>
                            </div>
                    )}
                        <Form onSubmit={this.handleLogin}>
                            <Modal.Body>
                                {defaultEnabled && <div className="error"><FormattedMessage {...messages.defaultUserEnabled} /></div>}
                                {error && <div className="error">{error}</div>}
                                <FormInput
                                    type="text"
                                    value={username}
                                    onChange={this.handleChange.bind(this, 'username')}
                                    label={this.props.intl.formatMessage(messages.username)}
                                    required
                                />
                                <FormInput
                                    type="password"
                                    value={password}
                                    onChange={this.handleChange.bind(this, 'password')}
                                    label={this.props.intl.formatMessage(messages.password)}
                                    required
                                />
                                <div className="submit-button">
                                    <Button
                                        type="submit"
                                        variant="outline-secondary"
                                        onClick={this.handleLogin}
                                        disabled={submitting}
                                    >
                                        <FormattedMessage {...messages.login} />
                                    </Button>
                                </div>
                            </Modal.Body>
                        </Form>
                    </ModalDialogWrapper>
                )}
            </div>
        );
    }
}

export default connect(state => {
    const {userDetail, login} = state;
    return {userDetail, login};
})(injectIntl(Login));
