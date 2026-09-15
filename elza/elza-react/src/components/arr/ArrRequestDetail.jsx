/**
 * Detail požadavku na externí systém.
 */

import React from 'react';
import {dateTimeToString} from '../../components/Utils';
import {connect} from 'react-redux';
import {AbstractReactComponent, StoreHorizontalLoader, Utils} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import { requestMessages, requestTypeMessages, daoRequestTypeMessages } from './requestMessages';
import FundNodesSelectForm from './FundNodesSelectForm';
import FundNodesList from './FundNodesList';
import NodeLabel from './NodeLabel';
import {modalDialogShow} from '../../actions/global/modalDialog';
import * as arrRequestActions from '../../actions/arr/arrRequestActions';
import RequestInlineForm from './RequestInlineForm';
import {DAO, DAO_LINK, DIGITIZATION, getRequestType} from './ArrUtils.jsx';
import {refExternalSystemsFetchIfNeeded} from '../../actions/refTables/externalSystems';
import {FormLabel} from 'react-bootstrap';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './ArrRequestDetailKeymap.jsx';
import { showConfirmDialog } from 'components/shared/dialog';

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
        return Promise.resolve();
    };

    handleAddNodes = () => {
        const {versionId, requestDetail} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                this.props.intl.formatMessage(requestMessages.fundNodesTitleSelect),
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

    handleRemoveNode = async (node) => {
        const {versionId, requestDetail, dispatch} = this.props;

        const response = await dispatch(showConfirmDialog(this.props.intl.formatMessage(requestMessages.fundNodesDeleteNode)))
        if (response) {
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
                        {<FormattedMessage {...requestMessages.requestTitleNodesDaosWithoutNode} />} ({countMap[NO_NODE_ID]})
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
                    <div className="title">{<FormattedMessage {...requestMessages.requestNoSelectionTitle} />}</div>
                    <div className="msg-text">{<FormattedMessage {...requestMessages.requestNoSelectionMessage} />}</div>
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
                    <h2>{<FormattedMessage {...requestMessages.requestTitleRequest} />}</h2>
                    <div className="form-group">
                        <label>{<FormattedMessage {...requestMessages.requestTitleCreated} />}</label> {dateTimeToString(new Date(req.create))}
                    </div>
                    {req.queued && (
                        <div className="form-group">
                            <label>{<FormattedMessage {...requestMessages.requestTitleQueued} />}</label> {dateTimeToString(new Date(req.queued))}
                        </div>
                    )}
                    {req.send && (
                        <div className="form-group">
                            <label>
                                {req.state === 'QUEUED'
                                    ? this.props.intl.formatMessage(requestMessages.requestTitleTrysend)
                                    : this.props.intl.formatMessage(requestMessages.requestTitleSend)}
                            </label>{' '}
                            {dateTimeToString(new Date(req.send))}
                        </div>
                    )}
                    <div className="form-group">
                        <label>{<FormattedMessage {...requestMessages.requestTitleType} />}</label> {this.props.intl.formatMessage(messageFor(requestTypeMessages, reqType, requestMessages.requestTitleTypeDIGITIZATION))}
                    </div>

                    <div className="form-group">
                        <label>{<FormattedMessage {...requestMessages.requestTitleDaoRequestSystem} />}</label> {extSystem ? extSystem.name : '-'}
                    </div>

                    {reqType === DAO && (
                        <div className="form-group">
                            <label>{<FormattedMessage {...requestMessages.requestTitleDaoRequestType} />}</label>{' '}
                            {this.props.intl.formatMessage(messageFor(daoRequestTypeMessages, req.type, requestMessages.requestTitleTypeDaoTRANSFER))}
                        </div>
                    )}

                    {reqType !== DAO_LINK && (
                        <RequestInlineForm
                            disabled={req.state !== 'OPEN'}
                            initialValues={req}
                            asyncValidate={this.handleSaveRequest}
                        />
                    )}

                    {
                        <div className="form-group">
                            <label>{<FormattedMessage {...requestMessages.requestTitleDaoRequestIdentifiersCode} />}</label> {req.code}
                        </div>
                    }

                    {req.externalSystemCode && (
                        <div className="form-group">
                            <label>{<FormattedMessage {...requestMessages.requestTitleDaoRequestIdentifiersExternalCode} />}</label>{' '}
                            {req.externalSystemCode}
                        </div>
                    )}

                    {reqType === DIGITIZATION && (
                        <div>
                            <label className="control-label">{<FormattedMessage {...requestMessages.requestTitleNodes} />}</label>
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
                            <label className="control-label">{<FormattedMessage {...requestMessages.requestTitleNodes} />}</label>
                            {this.renderDaoNodes(req)}
                        </div>
                    )}
                    {reqType === DAO_LINK && (
                        <div>
                            <label className="control-label">{<FormattedMessage {...requestMessages.requestTitleNodes} />}</label>
                            {this.renderDaoLinkNode(req)}
                        </div>
                    )}
                    {req.state === 'REJECTED' && req.rejectReason && (
                        <div>
                            <FormLabel>{<FormattedMessage {...requestMessages.requestTitleRejectReason} />}</FormLabel> {req.rejectReason}
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

export default connect(mapStateToProps)(injectIntl(ArrRequestDetail));
