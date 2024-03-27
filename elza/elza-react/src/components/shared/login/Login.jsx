import React from 'react';
import {connect} from 'react-redux';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {checkUserLogged, login} from 'actions/global/login.jsx';
import {WebApi} from 'actions/index.jsx';

import './Login.scss';
import {ModalDialogWrapper} from '../dialog/ModalDialogWrapper';
import FormInput from 'components/shared/form/FormInput';
import i18n from '../../i18n';
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
            this.setState({error: i18n('login.error.unknown')});
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

        return (
            <div className="login-container">
                {displayLoginDialog && (
                    <ModalDialogWrapper className="login" title={i18n('login.form.title')}>
                        <Form onSubmit={this.handleLogin}>
                            <Modal.Body>
                                {defaultEnabled && <div className="error">{i18n('login.defaultUserEnabled')}</div>}
                                {error && <div className="error">{error}</div>}
                                <FormInput
                                    type="text"
                                    value={username}
                                    onChange={this.handleChange.bind(this, 'username')}
                                    label={i18n('login.field.username')}
                                    required
                                />
                                <FormInput
                                    type="password"
                                    value={password}
                                    onChange={this.handleChange.bind(this, 'password')}
                                    label={i18n('login.field.password')}
                                    required
                                />
                                <div className="submit-button">
                                    <Button
                                        type="submit"
                                        variant="outline-secondary"
                                        onClick={this.handleLogin}
                                        disabled={submitting}
                                    >
                                        {i18n('login.action.login')}
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
})(Login);
