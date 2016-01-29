

require('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm} from 'components';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormDescItemTypeAdd, faSubNodeFormValueChange, faSubNodeFormDescItemTypeDelete, faSubNodeFormValueChangeSpec,faSubNodeFormValueBlur, faSubNodeFormValueFocus, faSubNodeFormValueAdd, faSubNodeFormValueDelete} from 'actions/arr/subNodeForm'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
var classNames = require('classnames');
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import DescItemString from './nodeForm/DescItemString'
import DescItemType from './nodeForm/DescItemType'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType, copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting'
import {addNode} from 'actions/arr/node'
import {createPacket} from 'actions/arr/packets'
import faSelectSubNode from 'actions/arr/nodes'

var SubNodeRegister = class SubNodeRegister extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods();

    }

    componentDidMount() {

    }

    render() {

        return (
            <div className='node-registers'>
                Ahooooj
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state
    return {
        nodeSettings: arrRegion.nodeSettings
    }
}

SubNodeRegister.propTypes = {
}

module.exports = connect(mapStateToProps)(SubNodeRegister);
