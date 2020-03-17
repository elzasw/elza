/**
 * Detail požadavku na externí systém.
 */

import React from 'react';
import {dateTimeToString} from 'components/Utils.jsx';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, StoreHorizontalLoader, Utils} from 'components/shared';
import FundNodesSelectForm from './FundNodesSelectForm';
import FundNodesList from './FundNodesList';
import NodeLabel from './NodeLabel';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import RequestInlineForm from './RequestInlineForm';
import {DAO, DAO_LINK, DIGITIZATION, getRequestType} from './ArrUtils.jsx';
import {refExternalSystemsFetchIfNeeded} from 'actions/refTables/externalSystems';
import {FormLabel} from 'react-bootstrap';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './ArrRequestDetailKeymap.jsx';

/**
 * Formulář detailu požadavku na digitalizaci.
 */
class ArrRequestDetail extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fund: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        ArrRequestDetail: PropTypes.object.isRequired,
    };

    componentDidMount() {
        const {versionId, requestDetail} = this.props;
        this.props.dispatch(refExternalSystemsFetchIfNeeded());

        requestDetail.id !== null &&
            this.props.dispatch(arrRequestActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {versionId, requestDetail} = nextProps;

        requestDetail.id !== null &&
            this.props.dispatch(arrRequestActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(nextProps);
    }

    trySetFocus = props => {
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

    handleShortcuts = action => {
        console.log('#handleShortcuts', '[' + action + ']', this);
    };

    handleSaveRequest = data => {
        const {versionId, requestDetail} = this.props;
        this.props.dispatch(arrRequestActions.requestEdit(versionId, requestDetail.id, data));
    };

    handleAddNodes = () => {
        const {versionId, requestDetail} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    onSubmitForm={(ids, nodes) => {
                        this.props.dispatch(
                            arrRequestActions.addNodes(
                                versionId,
                                requestDetail,
                                ids,
                                requestDetail.data.digitizationFrontdeskId,
                            ),
                        );
                    }}
                />,
            ),
        );
    };

    handleRemoveNode = node => {
        const {versionId, requestDetail} = this.props;

        if (window.confirm(i18n('arr.fund.nodes.deleteNode'))) {
            this.props.dispatch(arrRequestActions.removeNode(versionId, requestDetail, node.id));
        }
    };

    renderDaoLinkNode = req => {
        let nodeInfo;
        if (req.node) {
            const node = req.node;
            nodeInfo = <NodeLabel inline node={node} />;
        } else {
            nodeInfo = req.didCode;
        }

        return (
            <div>
                <div>{nodeInfo}</div>
            </div>
        );
    };

    renderDaoNodes = req => {
        const NO_NODE_ID = '---';
        // Mapa id node na node objekt
        const nodeMap = {};
        // Mapa id node na počet dao pod daným node
        const countMap = {};

        req.daos &&
            req.daos.forEach(dao => {
                let refId;
                if (dao.daoLink) {
                    const node = dao.daoLink.treeNodeClient;
                    nodeMap[node.id] = node;
                    refId = node.id;
                } else {
                    refId = NO_NODE_ID;
                }

                if (typeof countMap[refId] === 'undefined') {
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
                    nodesInfo.push(
                        <div>
                            {<NodeLabel inline node={node} />} ({countMap[node.id]})
                        </div>,
                    );
                } else {
                    nodesInfo.push(<div>{nodeId}</div>);
                }
            }
        });

        return (
            <div>
                {countMap[NO_NODE_ID] > 0 && (
                    <div>
                        {i18n('arr.request.title.nodes.daosWithoutNode')} ({countMap[NO_NODE_ID]})
                    </div>
                )}
                {nodesInfo}
            </div>
        );
    };

    render() {
        const {requestDetail, externalSystems} = this.props;

        let form;
        if (requestDetail.id === null) {
            form = (
                <div className="unselected-msg">
                    <div className="title">{i18n('arr.request.noSelection.title')}</div>
                    <div className="msg-text">{i18n('arr.request.noSelection.message')}</div>
                </div>
            );
        } else if (requestDetail.fetched) {
            const req = requestDetail.data;
            const reqType = getRequestType(req);

            let extSystem = {};
            if (externalSystems.fetched) {
                let externalSystemsMap = {};
                externalSystems.items.forEach(item => (externalSystemsMap[item.id] = item));
                if (reqType === DIGITIZATION) {
                    extSystem = externalSystemsMap[req.digitizationFrontdeskId];
                } else {
                    extSystem = externalSystemsMap[req.digitalRepositoryId];
                }
            }

            form = (
                <div>
                    <h2>{i18n('arr.request.title.request')}</h2>
                    <div className="form-group">
                        <label>{i18n('arr.request.title.created')}</label> {dateTimeToString(new Date(req.create))}
                    </div>
                    {req.queued && (
                        <div className="form-group">
                            <label>{i18n('arr.request.title.queued')}</label> {dateTimeToString(new Date(req.queued))}
                        </div>
                    )}
                    {req.send && (
                        <div className="form-group">
                            <label>
                                {req.state === 'QUEUED'
                                    ? i18n('arr.request.title.trysend')
                                    : i18n('arr.request.title.send')}
                            </label>{' '}
                            {dateTimeToString(new Date(req.send))}
                        </div>
                    )}
                    <div className="form-group">
                        <label>{i18n('arr.request.title.type')}</label> {i18n('arr.request.title.type.' + reqType)}
                    </div>

                    <div className="form-group">
                        <label>{i18n('arr.request.title.daoRequest.system')}</label> {extSystem ? extSystem.name : '-'}
                    </div>

                    {reqType === DAO && (
                        <div className="form-group">
                            <label>{i18n('arr.request.title.daoRequest.type')}</label>{' '}
                            {i18n('arr.request.title.type.dao.' + req.type)}
                        </div>
                    )}

                    {reqType !== DAO_LINK && (
                        <RequestInlineForm
                            disabled={req.state !== 'OPEN'}
                            reqType={reqType}
                            initData={req}
                            onSave={this.handleSaveRequest}
                        />
                    )}

                    {
                        <div className="form-group">
                            <label>{i18n('arr.request.title.daoRequest.identifiers.code')}</label> {req.code}
                        </div>
                    }

                    {req.externalSystemCode && (
                        <div className="form-group">
                            <label>{i18n('arr.request.title.daoRequest.identifiers.externalCode')}</label>{' '}
                            {req.externalSystemCode}
                        </div>
                    )}

                    {reqType === DIGITIZATION && (
                        <div>
                            <label className="control-label">{i18n('arr.request.title.nodes')}</label>
                            <FundNodesList
                                nodes={req.nodes}
                                onDeleteNode={this.handleRemoveNode}
                                onAddNode={this.handleAddNodes}
                                readOnly={req.state !== 'OPEN'}
                            />
                        </div>
                    )}
                    {reqType === DAO && (
                        <div>
                            <label className="control-label">{i18n('arr.request.title.nodes')}</label>
                            {this.renderDaoNodes(req)}
                        </div>
                    )}
                    {reqType === DAO_LINK && (
                        <div>
                            <label className="control-label">{i18n('arr.request.title.nodes')}</label>
                            {this.renderDaoLinkNode(req)}
                        </div>
                    )}
                    {req.state === 'REJECTED' && req.rejectReason && (
                        <div>
                            <FormLabel>{i18n('arr.request.title.rejectReason')}</FormLabel> {req.rejectReason}
                        </div>
                    )}
                </div>
            );
        }

        return (
            <Shortcuts name="ArrRequestDetail" handler={this.handleShortcuts}>
                {requestDetail.id !== null && <StoreHorizontalLoader store={requestDetail} />}
                <div className="arr-request-detail-container">{form}</div>
            </Shortcuts>
        );
    }
}

function mapStateToProps(state) {
    const {focus, userDetail, refTables} = state;
    return {
        externalSystems: refTables.externalSystems,
        focus,
        userDetail,
    };
}

export default connect(mapStateToProps)(ArrRequestDetail);
