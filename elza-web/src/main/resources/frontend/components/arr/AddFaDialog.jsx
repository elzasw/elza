/**
 * Dialog přidání nové AP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, ModalDialog, i18n} from 'components';
import {connect} from 'react-redux'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet'

var AddFaDialog = class AddFaDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods('');

        this.dispatch(refRuleSetFetchIfNeeded());

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
    console.log(this.props);

        return (
            <ModalDialog title={i18n('arr.fa.add')}>
xxx
            </ModalDialog>
        )
    }
}

module.exports = connect()(AddFaDialog);


