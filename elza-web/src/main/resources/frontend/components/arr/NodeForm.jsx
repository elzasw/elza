/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require ('./NodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'

var NodeForm = class NodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods('');

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
    console.log(this.props);

        return (
            <div className='node-form'>
cccccc
            </div>
        )
    }
}

module.exports = connect()(NodeForm);

