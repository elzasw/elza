import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {login, checkUserLogged} from 'actions/global/login.jsx';

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

    componentWillMount(){
        this.dispatch(checkUserLogged());
    }

    state = {
        ...this.defaultState
    };

    handleChange = (field, event) => {
        this.setState({[field]: event.target.value});
    };

    handleLoginError = (err) => {
        console.log(err);
        if (err.data && err.data.message) {
            this.setState({error: err.data.message});
        } else {
            this.setState({error: i18n('login.error.unknown')});
        }
    }

    handleLogin = (e) => {
        e.preventDefault();
        const {username, password} = this.state;

        this.dispatch(login(username, password)).then((data) => {
            this.setState(this.defaultState);
        }).catch((err) => {
            this.handleLoginError(err);
        });
    };

    render() {
        const {login, submitting, userDetail} = this.props;
        const {error, username, password} = this.state;

        // Login dialog is shown only when user is not logged in and data about user have been fetched
        // to prevent flicker on page reload
        const displayLoginDialog = !login.logged && userDetail.fetched;

        return <div className="login-container">
            {displayLoginDialog && <ModalDialogWrapper className="login" ref='wrapper' title={i18n('login.form.title')}>
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

export default connect((state) => {
        const {userDetail, login} = state;
        return {userDetail, login};
    }
)(Login);
