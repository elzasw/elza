import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {WebApi} from 'actions/index.jsx';
import {loginSuccess} from 'actions/global/login.jsx';

import './Login.less';
import ModalDialogWrapper from "../dialog/ModalDialogWrapper";
import FormInput from "../form/FormInput";
import i18n from "../../i18n";
import AbstractReactComponent from "../../AbstractReactComponent";

const defaultEnabled = typeof defaultUserEnabled !== "undefined" && defaultUserEnabled;

const getDefaultLogin = () => {
    if (defaultEnabled){
        return {
            username: "admin",
            password: "admin"
        };
    } else {
        return {
            username: "",
            password: ""
        };
    }
};


class Login extends AbstractReactComponent {
    defaultState = {
        ...getDefaultLogin(),
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
            console.log("xxxxx");
            this.dispatch(loginSuccess());
            try {
                login.callback && login.callback();
            } catch (ex) {
                console.error("Error calling login callback.", ex)
            }
            this.setState(this.defaultState);
        }).catch((err) => {
            console.log(err);
            if (err.data && err.data.message) {
                this.setState({error: err.data.message});
            } else {
                this.setState({error: i18n('login.error.unknown')});
            }
        });
    };

    render() {
        const {login, submitting} = this.props;
        const {error, username, password} = this.state;

        return <div className="login-container">
            {!login.logged && <ModalDialogWrapper className="login" ref='wrapper' title={i18n('login.form.title')}>
                <Form onSubmit={this.handleLogin}>
                    <Modal.Body>
                        {defaultEnabled && <div className="error">{i18n('login.defaultUserEnabled')}</div>}
                        {error && <div className="error">{error}</div>}
                        <FormInput type="text" value={username} onChange={this.handleChange.bind(this, 'username')} label={i18n('login.field.username')} />
                        <FormInput type="password" value={password} onChange={this.handleChange.bind(this, 'password')} label={i18n('login.field.password')} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={this.handleLogin} disabled={submitting}>{i18n('login.action.login')}</Button>
                    </Modal.Footer>
                </Form>
            </ModalDialogWrapper>}
        </div>;
    }
}

export default connect(({login}) => ({login}))(Login);
