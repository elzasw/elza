import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, Loading, NoFocusButton} from 'components';
import {connect} from 'react-redux'

// TODO slapa: dopsat

var SubNodeRegister = class SubNodeRegister extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderLink', 'renderForm', 'handleAddClick');

    }

    componentDidMount() {

    }

    handleAddClick() {

    }

    renderLink(link, index) {

        return (
                <div className="link" key={"link-" + index}>
                    {index}
                </div>
        );
    }

    renderForm() {
        const {register} = this.props;

        var links = [];
        register.data.forEach((link, index) => links.push(this.renderLink(link, index)));

        return (
                <div className="form">
                    {links}
                    <div className='action'><NoFocusButton onClick={this.handleAddClick}><Icon glyph="fa-plus" /></NoFocusButton></div>
                </div>
        );

    }

    render() {

        const {register} = this.props;

        var form;

        if (register.fetched) {
            form = this.renderForm();
        } else {
            form = <Loading value={i18n('global.data.loading.register')} />
        }

        return (
            <div className='node-registers'>
                {form}
            </div>
        )
    }
}

SubNodeRegister.propTypes = {
    register: React.PropTypes.object.isRequired
}

module.exports = connect()(SubNodeRegister);
