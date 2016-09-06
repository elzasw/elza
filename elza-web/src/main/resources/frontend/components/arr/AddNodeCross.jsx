/**
 * Interaktivní tlačítko pro určení směru přidání JP
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import {isFundRootId} from './ArrUtils.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {getOneSettings} from 'components/arr/ArrUtils.jsx';
import {AbstractReactComponent, Icon, i18n, Loading, AddNodeForm} from 'components/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';

require ('./AddNodeCross.less');

const AddNodeCross = class AddNodeCross extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddNode');
    }

    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        let itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            const nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.nodeId);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }

    /**
     * Přidání podřízeného záznamu
     */
    handleAddNode(direction) {
        const {node, versionId} = this.props;

        //this.dispatch(addNode(selectedSubNode, parentNode, this.props.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
        this.dispatch(modalDialogShow(this, i18n('arr.fund.addNode'),
                <AddNodeForm node={node} versionId={versionId} initDirection={direction} handleSubmit={(e) => (console.log(e))}/>
            )
        )
    }

    renderCross() {
        const notRoot = !isFundRootId(this.props.node.id);
        return (
            <div className="hid">
                {notRoot &&
                    [<div key="addBefore" className="but top" onClick={this.handleAddNode.bind(this,'BEFORE')}>
                        <span className="ico fa fa-arrow-up"></span><br/>{i18n('arr.fund.addNode.before')}</div>,
                    <div key="addAfter" className="but bottom" onClick={this.handleAddNode.bind(this,'AFTER')}>
                        <span className="ico fa fa-arrow-down"></span><br/>{i18n('arr.fund.addNode.after')}</div>,
                    <div key="addAtEnd" className="but bottom2" onClick={this.handleAddNode.bind(this,'ATEND')}>
                        <span className="ico fa fa-arrow-down"></span><br/>{i18n('arr.fund.addNode.atEnd')}</div>]
                }
                <div key="addChild" className="but right" onClick={this.handleAddNode.bind(this,'CHILD')}>
                    <span className="ico fa fa-level-up fa-rotate-90"></span><br/>{i18n('arr.fund.addNode.child')}</div>
            </div>
        )
    }

    render() {
        const { userDetail, fundId, closed} = this.props;
        let formActions

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var settingsValues = settings.value != 'false';
        const readMode = closed || settingsValues;
        var active = false;

        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            if (!readMode) {
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
    node: React.PropTypes.any.isRequired,
    userDetail: React.PropTypes.object.isRequired
};
module.exports = connect()(AddNodeCross);
