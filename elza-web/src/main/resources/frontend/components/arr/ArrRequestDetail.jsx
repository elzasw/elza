/**
 * Detail požadavku na externí systém.
 */

import React from 'react';
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import Utils, {dateTimeToString} from "components/Utils.jsx";
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {
    Loading,
    i18n,
    OutputSubNodeForm,
    FundNodesSelectForm,
    FundNodesList,
    AbstractReactComponent,
    FormInput,
    NodeLabel
} from 'components/index.jsx';
import {fundOutputDetailFetchIfNeeded, fundOutputEdit} from 'actions/arr/fundOutput.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundOutputRemoveNodes, fundOutputAddNodes} from 'actions/arr/fundOutput.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import RequestInlineForm from "./RequestInlineForm";
import {DIGITIZATION, DAO, DAO_LINK, getRequestType} from './ArrUtils.jsx'
import {refExternalSystemsFetchIfNeeded} from 'actions/refTables/externalSystems';

const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
const keyModifier = Utils.getKeyModifier();

const keymap = {
    ArrRequestDetail: {
        xxx: keyModifier + 'e',
    },
};
const shortcutManager = new ShortcutsManager(keymap);

/**
 * Formulář detailu požadavku na digitalizaci.
 */
class ArrRequestDetail extends AbstractReactComponent {

    static PropTypes = {
        versionId: React.PropTypes.number.isRequired,
        fund: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        ArrRequestDetail: React.PropTypes.object.isRequired,
    };

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        const {versionId, requestDetail} = this.props;
        this.dispatch(refExternalSystemsFetchIfNeeded());

        requestDetail.id !== null && this.dispatch(arrRequestActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, requestDetail} = nextProps;

        requestDetail.id !== null && this.dispatch(arrRequestActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(nextProps)
    }

    trySetFocus = (props) => {
        //let {focus} = props;

        // if (canSetFocus()) {
        //     if (isFocusFor(focus, 'fund-output', 1)) {
        //         this.refs.fundOutputList && this.setState({}, () => {
        //             ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
        //         })
        //         focusWasSet()
        //     }
        // }
    };

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
    };

    getChildContext() {
        return {shortcuts: shortcutManager};
    }

    handleSaveRequest = (data) => {
        const {versionId, requestDetail} = this.props;
        this.dispatch(arrRequestActions.requestEdit(versionId, requestDetail.id, data));
    };

    handleAddNodes = () => {
        const {versionId, requestDetail} = this.props;
        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                onSubmitForm={(ids, nodes) => {
                    this.dispatch(arrRequestActions.addNodes(versionId, requestDetail, ids, requestDetail.data.digitizationFrontdeskId))
                }}
            />))
    };

    handleRemoveNode = (node) => {
        const {versionId, requestDetail} = this.props;

        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            this.dispatch(arrRequestActions.removeNode(versionId, requestDetail, node.id))
        }
    };

    renderDaoLinkNode = (req) => {
        let nodeInfo;
        if (req.node) {
            const node = req.node;
            nodeInfo = <NodeLabel inline node={node}/>;
        } else {
            nodeInfo = req.didCode;
        }

        return <div>
            <div>
                {nodeInfo}
            </div>
        </div>
    };

    renderDaoNodes = (req) => {
        const NO_NODE_ID = "---";
        // Mapa id node na node objekt
        const nodeMap = {};
        // Mapa id node na počet dao pod daným node
        const countMap = {};

        req.daos && req.daos.forEach(dao => {
            let refId;
            if (dao.daoLink) {
                const node = dao.daoLink.treeNodeClient;
                nodeMap[node.id] = node;
                refId = node.id;
            } else {
                refId = NO_NODE_ID;
            }

            if (typeof countMap[refId] === "undefined") {
                countMap[refId] = 1;
            } else {
                countMap[refId]++;
            }
        });

        const nodesInfo = [];
        Object.keys(countMap).forEach(nodeId => {
            if (nodeId !== NO_NODE_ID) {
                const node = nodeMap[nodeId];
                if (node) {
                    nodesInfo.push(<div>
                        {<NodeLabel inline node={node}/>} ({countMap[node.id]})
                    </div>);
                } else {
                    nodesInfo.push(<div>
                        {nodeId}
                    </div>);
                }

            }
        });

        return <div>
            {countMap[NO_NODE_ID] > 0 && <div>
                {i18n("arr.request.title.nodes.daosWithoutNode")} ({countMap[NO_NODE_ID]})
            </div>}
            {nodesInfo}
        </div>
    };

    render() {
        const {requestDetail, externalSystems} = this.props;

        let form;
        if (requestDetail.id === null) {
            form = <div className="unselected-msg">
                <div className="title">{i18n('arr.request.noSelection.title')}</div>
                <div className="msg-text">{i18n('arr.request.noSelection.message')}</div>
            </div>;
        } else if (requestDetail.fetched && externalSystems.fetched) {

            let externalSystemsMap = {};

            externalSystems.items.forEach((item) => externalSystemsMap[item.id] = item);

            const req = requestDetail.data;
            const reqType = getRequestType(req);

            let extSystem = {};
            if (reqType === DIGITIZATION) {
                extSystem = externalSystemsMap[req.digitizationFrontdeskId];
            } else {
                extSystem = externalSystemsMap[req.digitalRepositoryId]
            }

            form = (
                <div>
                    <h2>{i18n("arr.request.title.request")}</h2>
                    <div className="form-group">
                        <label>{i18n("arr.request.title.created")}</label> {dateTimeToString(new Date(req.create))}
                    </div>
                    {req.queued && <div className="form-group">
                        <label>{i18n("arr.request.title.queued")}</label> {dateTimeToString(new Date(req.queued))}
                    </div>}
                    {req.send && <div className="form-group">
                        <label>{req.state == "QUEUED" ? i18n("arr.request.title.trysend") : i18n("arr.request.title.send")}</label> {dateTimeToString(new Date(req.send))}
                    </div>}
                    <div className="form-group">
                        <label>{i18n("arr.request.title.type")}</label> {i18n("arr.request.title.type." + reqType)}
                    </div>

                    <div className="form-group">
                        <label>{i18n("arr.request.title.daoRequest.system")}</label> {extSystem.name}
                    </div>

                    {reqType === DAO && <div className="form-group">
                        <label>{i18n("arr.request.title.daoRequest.type")}</label> {i18n("arr.request.title.type.dao." + req.type)}
                    </div>}

                    {reqType !== DAO_LINK && <RequestInlineForm
                        disabled={req.state != "OPEN"}
                        reqType={reqType}
                        initData={req}
                        onSave={this.handleSaveRequest}
                    />}

                    {reqType === DIGITIZATION && <div>
                        <label className="control-label">{i18n("arr.request.title.nodes")}</label>
                        <FundNodesList
                            nodes={req.nodes}
                            onDeleteNode={this.handleRemoveNode}
                            onAddNode={this.handleAddNodes}
                            readOnly={req.state != "OPEN"}
                        />
                    </div>}
                    {reqType === DAO && <div>
                        <label className="control-label">{i18n("arr.request.title.nodes")}</label>
                        {this.renderDaoNodes(req)}
                    </div>}
                    {reqType === DAO_LINK && <div>
                        <label className="control-label">{i18n("arr.request.title.nodes")}</label>
                        {this.renderDaoLinkNode(req)}
                    </div>}
                </div>
            )
        } else {
            form = <Loading value={i18n('global.data.loading')}/>;
        }

        return <Shortcuts name='ArrRequestDetail' handler={this.handleShortcuts}>
            <div className='arr-request-detail-container'>
                {form}
            </div>
        </Shortcuts>;
    }
}

function mapStateToProps(state) {
    const {focus, userDetail, refTables} = state;
    return {
        externalSystems: refTables.externalSystems,
        focus,
        userDetail,
    }
}

export default connect(mapStateToProps)(ArrRequestDetail);
