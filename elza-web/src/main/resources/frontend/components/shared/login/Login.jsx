var React = require('react');

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, ModalDialogWrapper} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils'
import {WebApi} from 'actions'
import {loginSuccess} from 'actions/global/login';

require('./Login.less');

// TODO: smazat až bude potřeba
const defaultLogin = {
    username: "admin",
    password: "admin"
}

var Login = class extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.bindMethods('handleLogin');

        this.state = {username: defaultLogin.username, password: defaultLogin.password, "error": null}
    }

    handleChange(field, event) {
        var state = this.state;
        state[field] = event.target.value;
        this.setState(state);
    }

    handleLogin() {
        const {login} = this.props;

        // volám nepřetížený api
        WebApi._login(this.state.username, this.state.password).then((data)=>{
            this.dispatch(loginSuccess());
            login.callback();
            this.setState({username: defaultLogin.username, password: defaultLogin.password, "error": null});
        }).catch((err)=>{
            var state = this.state;
            state['error'] = err.data.message;
            this.setState(state);
        });
    }

    render() {
        const {login} = this.props;
        var error = this.state.error && <div className="error">{this.state.error}</div>

        var dialog = !login.logged &&
        <ModalDialogWrapper className="login" ref='wrapper' title={i18n('login.form.title')} onHide={()=>{console.log("close")}}>
            <form onSubmit={this.handleLogin}>
                <Modal.Body>
                    {error}
                        <Input type="text" value={this.state.username} onChange={this.handleChange.bind(this, "username")} label={i18n('login.field.username')} />
                        <Input type="password" value={this.state.password} onChange={this.handleChange.bind(this, "password")} label={i18n('login.field.password')} />
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleLogin}>{i18n('login.action.login')}</Button>
                </Modal.Footer>
            </form>
        </ModalDialogWrapper>

        return (
            <div className="login-container">
                {dialog}
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {login} = state
    return {
        login
    }
}

module.exports = connect(mapStateToProps)(Login);