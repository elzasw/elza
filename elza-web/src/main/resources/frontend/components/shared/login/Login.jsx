import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, ModalDialogWrapper, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions/index.jsx';
import {loginSuccess} from 'actions/global/login.jsx';

import './Login.less';

// TODO: smazat až bude potřeba
const defaultLogin = {
    username: "admin",
    password: "admin"
};

class Login extends AbstractReactComponent {
    defaultState = {
        username: '',
        password: '',
        ...defaultLogin, // TODO: Smazat až bude potřeba
        error: null
    };

    state = {
        ...this.defaultState
    };

    handleChange = (field, event) => {
        this.setState({[field]: event.target.value});
    };

    handleLogin = (e) => {
        e.preventDefault();
        const {login} = this.props;
        const {username, password} = this.state;

        // volám nepřetížený api
        WebApi._login(username, password).then((data) => {
            this.dispatch(loginSuccess());
            try {
                login.callback && login.callback();
            } catch (ex) {
                console.error("Error calling login callback.", ex)
            }
            this.replaceState(this.defaultState);
        }).catch((err) => {
            this.setState({error: err.data.message});
        });
    };

    render() {
        const {login} = this.props;
        const {error, username, password} = this.state;

        return <div className="login-container">
            {!login.logged && <ModalDialogWrapper className="login" ref='wrapper' title={i18n('login.form.title')}>
                <Form onSubmit={this.handleLogin}>
                    <Modal.Body>
                        {error && <div className="error">{error}</div>}
                        <FormInput type="text" value={username} onChange={this.handleChange.bind(this, 'username')} label={i18n('login.field.username')} />
                        <FormInput type="password" value={password} onChange={this.handleChange.bind(this, 'password')} label={i18n('login.field.password')} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={this.handleLogin}>{i18n('login.action.login')}</Button>
                    </Modal.Footer>
                </Form>
            </ModalDialogWrapper>}
        </div>;
    }
}

export default connect(({login}) => ({login}))(Login);