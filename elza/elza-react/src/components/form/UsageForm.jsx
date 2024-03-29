import * as types from 'actions/constants/ActionTypes';
import { TooltipTrigger } from 'components/shared';
import { showConfirmDialog } from 'components/shared/dialog';
import Icon from 'components/shared/icon/FontIcon';
import PropTypes from 'prop-types';
import React from 'react';
import { Modal } from 'react-bootstrap';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { objectById, storeFromArea } from 'shared/utils';
import { usageFundTreeReceive } from '../../actions/arr/globalFundTree';
import { modalDialogHide } from '../../actions/global/modalDialog';
import { AREA_REGISTRY_LIST } from '../../actions/registry/registry';
import * as perms from '../../actions/user/Permission';
import FundTreeUsage from '../arr/FundTreeUsage';
import i18n from '../i18n';
import RegistryField from '../registry/RegistryField';
import ToggleContent from '../shared/toggle-content/ToggleContent';
import { Button } from '../ui';
import './UsageForm.scss';
import { routerNavigate } from 'actions/router';
import { urlNode } from '../../constants';

const EntityDisplay = ({
    scope, 
    id, 
    uuid, 
    title
}) => {
    return <div className="entity">
        <div className="title">{title}</div>
        <div className="info">
            <TooltipTrigger 
                content={
                <>
                    <div>id: {id}</div>
                    <div>uuid: {uuid}</div>
                    </>
            }
            >
                <div className="info-item">id: {id}</div>
            </TooltipTrigger>
            <div className="info-item">
                <Icon glyph={'fa-globe'}/> {scope}
            </div>
        </div>
    </div>
}

class RegistryUsageForm extends React.Component {
    static propTypes = {
        detail: PropTypes.object,
        replaceText: PropTypes.string,
        replaceButtonText: PropTypes.string,
        mergeButtonText: PropTypes.string,
        replaceType: PropTypes.string,
        nameLabel: PropTypes.string,
    };

    static defaultProps = {
        replaceText: i18n("registry.replaceText"),
        replaceButtonText: i18n('registry.replace'),
        mergeButtonText: i18n('registry.merge'),
        replaceType: "replace",
    }

    rootFundIdfOffset = 0.1;
    manyItemsIdOffset = 0.4;
    manyItemsLabel = i18n('registry.usage.tooMany');
    expandFundThreshold = 500;

    state = {
        selectedReplacementNode: null,
        usageCount: 0,
        data: {},
    };

    treeRef = null;

    componentDidMount() {
        const {detail, data} = this.props;
        if (detail.id) {
            if (data) {
                this.setState({
                    usageCount: this.countOccurences(data),
                    data,
                });

                this.props.dispatch(
                    usageFundTreeReceive([...this.formatDataForTree(data.funds)], this.getDefaultExpandedIds(data)),
                );
            }
        }
    }

    getDefaultExpandedIds(data) {
        let expnadedIds = {};

        data.funds.forEach(fund => {
            if (fund.nodes.length < this.expandFundThreshold) {
                expnadedIds[fund.id + this.rootFundIdfOffset] = true;
            }
        });

        return expnadedIds;
    }

    countOccurences = data => {
        return data.funds.reduce((sum, fund) => sum + fund.nodes.length, 0);
    };

    countOccurencesInAS = as => {
        return as.reduce((sum, jp) => {
            return sum + this.countOccurencesForNode(jp);
        }, 0);
    };

    countOccurencesForNode = node => node.occurrences && node.occurrences.length;

    formatDataForTree(items) {
        const processedFunds = [];
        items.forEach(item => {
            processedFunds.push({
                id: item.id + this.rootFundIdfOffset, //proti překrytí id, míchání dvou druhů dat do jedné komponenty
                propertyId: item.id, //původní id
                type: 'fund',
                icon: 'fa-database',
                name: item.name,
                depth: 1,
                hasChildren: true,
                count: item.nodes && this.countOccurencesInAS(item.nodes),
                link: false,
            });
            if (item.nodes) {
                if (item.nodes.length < this.expandFundThreshold) {
                    processedFunds.push(
                        ...item.nodes.map(node => ({
                            name: node.title,
                            type: 'fund',
                            depth: 2,
                            icon: 'fa-fw',
                            id: node.id, //proti překrytí id, míchání dvou druhů dat do jedné komponenty
                            propertyId: node.id, //původní id
                            parent: item.id + this.rootFundIdfOffset,
                            origParent: item.id,
                            link: true,
                        })),
                    );
                } else {
                    processedFunds.push({
                        id: item.id + this.manyItemsIdOffset,
                        depth: 2,
                        name: this.manyItemsLabel,
                        icon: 'fa-fw',
                    });
                }
            }
        });

        return processedFunds;
    }

    expandNode(node) {
        const {fundTreeUsage} = this.props;

        const nodes = [...this.formatDataForTree(this.state.data.funds)];

        const expandedNodes = nodes.filter(nodeItem => {
            if (
                nodeItem.id === node.id ||
                nodeItem.parent === node.id ||
                fundTreeUsage.expandedIds[nodeItem.id] === true ||
                fundTreeUsage.expandedIds[nodeItem.parent] === true ||
                nodeItem.hasChildren
            ) {
                return true;
            }
            return false;
        });

        this.props.dispatch({
            type: types.FUND_FUND_TREE_RECEIVE,
            area: this.props.treeArea,
            nodes: expandedNodes,
            expandedIds: fundTreeUsage.expandedIds,
            expandedIdsExtension: [],
        });

        this.props.dispatch({
            type: types.FUND_FUND_TREE_EXPAND_NODE,
            area: this.props.treeArea,
            node,
        });
    }

    collapseNode(node) {
        this.props.dispatch({
            type: types.FUND_FUND_TREE_COLLAPSE_NODE,
            area: this.props.treeArea,
            node,
        });
    }

    handleOpenCloseNode = (node, expand) => {
        if (expand) {
            this.expandNode(node);
        } else {
            this.collapseNode(node);
        }
    };

    handleChoose = selectedReplacementNode => {
        this.setState({selectedReplacementNode});
    };

    canReplace() {
        const {userDetail, detail} = this.props;
        const {selectedReplacementNode} = this.state;

        if (selectedReplacementNode && detail.id !== selectedReplacementNode.id) {
            return userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                type: perms.AP_SCOPE_WR,
                scopeId: selectedReplacementNode.scopeId,
            });
        }
        return false;
    }

    handleLinkClick = node => {
        const { dispatch, history } = this.props;
        dispatch(modalDialogHide());
        if (node.type === 'fund') {
            dispatch(routerNavigate(urlNode(node.id)));
        }
    };

    renderReplaceField = () => {
        const { type } = this.props;
        const {selectedReplacementNode} = this.state;
        return (
            <div className="field-container">
                {type === 'registry' && (
                    <RegistryField
                        value={selectedReplacementNode}
                        onChange={this.handleChoose}
                        onBlur={() => {}}
                    />
                )}
            </div>
        )
    }

    getScope = (scopeId) => {
        const { scopes } = this.props;
        if (scopeId != undefined) {
            return objectById(scopes, scopeId);
        }
        return undefined;
    }

    confirmReplaceOrMerge = async (action) => {
        const { dispatch, detail } = this.props;
        const { selectedReplacementNode } = this.state;

        const scope = this.getScope(detail?.data?.scopeId);
        const replacementScope = this.getScope(selectedReplacementNode?.scopeId);

        console.log(detail, selectedReplacementNode);
        const response = await dispatch(showConfirmDialog(<div className="confirmation">
            <div>{i18n(`accesspoint.removeDuplicity.confirm.${action}.a`)}</div>
            <EntityDisplay 
                title={detail?.data?.name}
                id={detail?.data?.id}
                uuid={detail?.data?.uuid}
                scope={scope.name}
                />
            <div>{i18n(`accesspoint.removeDuplicity.confirm.${action}.b`)}</div>
            <EntityDisplay 
                title={selectedReplacementNode.name}
                id={selectedReplacementNode.id}
                uuid={selectedReplacementNode.uuid}
                scope={replacementScope?.name}
                />
            {detail?.data?.name !== selectedReplacementNode.name && <div className="error">
                <Icon glyph="fa-exclamation-circle"/>&nbsp;{i18n("accesspoint.removeDuplicity.confirm.nonEqualNames")}
            </div>}
        </div>));

        return response;
    }

    handleReplace = async () => {
        const { onReplace } = this.props;
        const { selectedReplacementNode } = this.state;
        const response = await this.confirmReplaceOrMerge("replace");

        if(response){ onReplace(selectedReplacementNode); }
    }

    handleMerge = async () => {
        const { onMerge } = this.props;
        const { selectedReplacementNode } = this.state;
        const response = await this.confirmReplaceOrMerge("merge");

        if(response){ onMerge(selectedReplacementNode); }
    }

    render() {
        const {detail, fundTreeUsage, replaceType, onReplace, onMerge, nameLabel} = this.props;
        const { replaceButtonText, mergeButtonText} = this.props;

        const canReplace = (replaceType === "replace" && this.state.usageCount > 0);
        const canDelete = replaceType === "delete";

        return (
            <>
            <Modal.Body className="reg-usage-form">
                {nameLabel && <div className="name-label">{nameLabel}</div>}
                <h4>{detail && detail.data && detail.data.name}</h4>
                { onReplace && onMerge && (canDelete ? 
                    <div className="actions-container">
                        <div className="actions-text">{this.props.replaceText}</div>
                        {this.renderReplaceField()}
                    </div> : 
                    canReplace && (
                        <ToggleContent 
                            withText 
                            text={this.props.replaceText} 
                        >
                            {this.renderReplaceField()}
                        </ToggleContent>
                    ))}
                <ToggleContent 
                    withText 
                    opened={!canDelete}
                    text={`${i18n('registry.registryUsageCount')} ${this.state.usageCount}`}
                >
                    {fundTreeUsage && (
                        <FundTreeUsage
                            handleOpenCloseNode={this.handleOpenCloseNode}
                            className="fund-tree-container-fixed"
                            ref={ref => (this.treeRef = ref)}
                            showCountStats={true}
                            onLinkClick={this.handleLinkClick}
                            {...fundTreeUsage}
                            />
                    )}
                </ToggleContent>
            </Modal.Body>
                {canDelete &&
                    <Modal.Footer>
                        <Button
                            onClick={this.handleReplace}
                            disabled={!this.canReplace() || !onReplace}
                            variant="outline-secondary"
                        >
                            {replaceButtonText}
                        </Button>
                        <Button
                            onClick={this.handleMerge}
                            disabled={!this.canReplace() || !onMerge}
                            variant="outline-secondary"
                        >
                            {mergeButtonText}
                        </Button>
                    </Modal.Footer>
                }
            </>
        );
    }

    componentWillUnmount() {
        this.props.dispatch({
            type: types.FUND_FUND_TREE_INVALIDATE,
        });
    }
}

export default withRouter(
    connect(state => {
        const registryList = storeFromArea(state, AREA_REGISTRY_LIST);

        return {
            fundTreeUsage: state.arrRegion.globalFundTree.fundTreeUsage,
            registryList,
            userDetail: state.userDetail,
            scopes: state.refTables.scopesData.scopes.find((scope) => scope.versionId === -1)?.scopes || [],
        };
    })(RegistryUsageForm),
);
