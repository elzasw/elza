/**
 * Interaktivní tlačítko pro určení směru přidání JP
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {addNodeFormArr} from 'actions/arr/addNodeForm.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {AbstractReactComponent, i18n} from 'components/shared';
import {getOneSettings, isFundRootId} from 'components/arr/ArrUtils.jsx';

import './AddNodeCross.less';

const AddNodeCross = class AddNodeCross extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddNode');
    }

    /**
     * Zavolání dialogu pro přidání záznamu s předdefinovaným směrem
     */
    handleAddNode(direction) {
        const {node, selectedSubNodeIndex, versionId} = this.props;
        this.dispatch(addNodeFormArr(direction, node, selectedSubNodeIndex, versionId));
    }

    renderCross() {
        const notRoot = !isFundRootId(this.props.node.id);
        return (
            <div className="hid">
                {notRoot &&
                    [<div key="addBefore" className="but top" onClick={this.handleAddNode.bind(this,'BEFORE')}>
                        <div className="ico"><span className="fa fa-arrow-up"></span></div><div className="lbl">{i18n('arr.fund.addNode.before')}</div></div>,
                    <div key="addAfter" className="but bottom" onClick={this.handleAddNode.bind(this,'AFTER')}>
                        <div className="ico"><span className="fa fa-arrow-down"></span></div><div className="lbl">{i18n('arr.fund.addNode.after')}</div></div>,
                    <div key="addAtEnd" className="but bottom2" onClick={this.handleAddNode.bind(this,'ATEND')}>
                        <div className="ico"><span className="fa fa-arrow-down"></span></div><div className="lbl">{i18n('arr.fund.addNode.atEnd')}</div></div>]
                }
                <div key="addChild" className="but right" onClick={this.handleAddNode.bind(this,'CHILD')}>
                    <div className="ico"><span className="fa fa-level-up fa-rotate-90"></span></div><div className="lbl">{i18n('arr.fund.addNode.child')}</div></div>
            </div>
        )
    }

    render() {
        const { userDetail, fundId, closed, arrPerm} = this.props;
        let formActions

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var settingsValues = settings.value != 'false';
        const readMode = closed || settingsValues;
        var active = false;

        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) || arrPerm) {
            if (!readMode && arrPerm) {
                active = true;
                formActions = this.renderCross();
            }
        }

        return (
            <div className="con2">
                <div className="cont">
                    <div className={active ? "but center blue-but":"but center"}><span className="ico fa fa-plus"></span></div>
                    {formActions}
                </div>
            </div>
        );
    }
};

AddNodeCross.propTypes = {
    node: PropTypes.any.isRequired,
    userDetail: PropTypes.object.isRequired,
    selectedSubNodeIndex: PropTypes.number.isRequired
};
export default connect()(AddNodeCross);
