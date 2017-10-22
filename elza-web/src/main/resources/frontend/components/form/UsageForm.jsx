import React from 'react';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {WebApi} from '../../actions/WebApi';
import {connect} from 'react-redux';
import {usageFundTreeReceive} from '../../actions/arr/globalFundTree';
import FundTreeUsage from '../arr/FundTreeUsage';
import './UsageForm.less';
import RegistryField from '../registry/RegistryField';
import * as types from 'actions/constants/ActionTypes.js';
import ToggleContent from '../shared/toggle-content/ToggleContent';
import {AREA_PARTY_LIST, partyDetailClear, partyDetailFetchIfNeeded, partyListFilter} from '../../actions/party/party';
import {
    AREA_REGISTRY_LIST,
    registryDetailClear,
    registryDetailFetchIfNeeded,
    registryListFilter
} from '../../actions/registry/registry';
import {modalDialogShow} from '../../actions/global/modalDialog';
import RegistrySelectPage from 'pages/select/RegistrySelectPage.jsx'
import classNames from 'classnames';
import {MODAL_DIALOG_VARIANT} from 'constants.jsx';
import storeFromArea from '../../shared/utils/storeFromArea';
import objectById from '../../shared/utils/objectById';
import i18n from '../i18n';
import PartyField from "../party/PartyField";
import PartySelectPage from "../../pages/select/PartySelectPage";
import * as perms from "../../actions/user/Permission";
import {decorateAutocompleteValue} from "../arr/nodeForm/DescItemUtils";

class RegistryUsageForm extends React.Component {
    static propTypes = {
        detail: React.PropTypes.object
    };

    rootFundIdfOffset = 0.1;
    rootPartyIdOffset = 0.2;
    nodePartyIdOffset = 0.3;
    expandFundThreshold = 500;

    state = {
        selectedNode: null,
        selectedReplacementNode: null,
        usageCount: 0,
        data: {},
    };

    componentDidMount() {
        const {detail, data} = this.props;
        if (detail.id) {
            //WebApi.findRegistryUsage(detail.id).then(data => {
                if (data) {
                    this.setState({
                        usageCount: this.countOccurences(data),
                        data
                    });

                    this.props.dispatch(
                        usageFundTreeReceive(
                            [
                                ...this.formatDataForTree(data.funds, 'fund'),
                                ...this.formatDataForTree(data.parties, 'party')
                            ],
                            this.getDefaultExpandedIds(data)
                        )
                    );
                }
            //});
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
        return data.funds.reduce((sum, fund) => sum + fund.nodes.length, 0) + data.parties.length;
    };

    countOccurencesForNode = node => node.occurrences && node.occurrences.length;

    formatDataForTree(items, type) {
        const processedFunds = [];
        items.forEach(item => {
            processedFunds.push({
                id: item.id + (type === 'fund' ? this.rootFundIdfOffset : this.rootPartyIdOffset), //proti překrytí id, míchání dvou druhů dat do jedné komponenty
                propertyId: item.id, //původní id
                type,
                icon: type === 'fund' ? 'fa-database' : 'fa-users',
                name: item.name,
                depth: 1,
                hasChildren: type === 'fund',
                count: type === 'fund' ? item.nodes && item.nodes.length : this.countOccurencesForNode(item)
            });
            if (item.nodes) {
                processedFunds.push(
                    ...item.nodes.map(node => ({
                        ...node,
                        name: node.title,
                        type,
                        depth: 2,
                        icon: 'fa-fw',
                        id: item.type === 'party' ? node.id + this.nodePartyIdOffset : node.id, //proti překrytí id, míchání dvou druhů dat do jedné komponenty
                        propertyId: node.id, //původní id
                        parent: item.id + this.rootFundIdfOffset
                        //count: this.countOccurencesForNode(node)
                    }))
                );
            }
        });

        return processedFunds;
    }

    handleNodeClick = selectedNode => {
        this.setState({selectedNode});
    };

    expandNode(node) {
        const {fundTreeUsage} = this.props;

        const nodes = [
            ...this.formatDataForTree(this.state.data.funds, 'fund'),
            ...this.formatDataForTree(this.state.data.parties, 'party')
        ];

        const expandedNodes = nodes.filter(nodeItem => {
            if (
                nodeItem.id === node.id ||
                nodeItem.parent === node.id ||
                fundTreeUsage.expandedIds[nodeItem.id] === true ||
                fundTreeUsage.expandedIds[nodeItem.parent] === true ||
                nodeItem.hasChildren ||
                nodeItem.type === 'party'
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
            expandedIdsExtension: []
        });

        this.props.dispatch({
            type: types.FUND_FUND_TREE_EXPAND_NODE,
            area: this.props.treeArea,
            node
        });
    }

    collapseNode(node) {
        this.props.dispatch({
            type: types.FUND_FUND_TREE_COLLAPSE_NODE,
            area: this.props.treeArea,
            node
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

    handleSelectModuleRegistry = ({onSelect, filterText, value}) => {
        const {
            hasSpecification,
            descItem,
            registryList,
            partyList,
        } = this.props;
        const open = (hasParty = false) => {
            if (hasParty) {
                this.props.dispatch(
                    partyListFilter({
                        ...partyList.filter,
                        text: filterText,
                        itemSpecId: hasSpecification ? descItem.descItemSpecId : null
                    })
                );
                this.props.dispatch(partyDetailClear());
            }
            this.props.dispatch(
                registryListFilter({
                    ...registryList.filter,
                    text: filterText,
                    itemSpecId: hasSpecification ? descItem.descItemSpecId : null
                })
            );
            this.props.dispatch(registryDetailFetchIfNeeded(value ? value.id : null));
            this.props.dispatch(
                modalDialogShow(
                    this,
                    null,
                    <RegistrySelectPage
                        hasParty={hasParty}
                        onSelect={data => {
                            onSelect(data);
                            if (hasParty) {
                                this.props.dispatch(partyListFilter({text: null, type: null, itemSpecId: null}));
                                this.props.dispatch(partyDetailClear());
                            }
                            this.props.dispatch(
                                registryListFilter({
                                    text: null,
                                    registryParentId: null,
                                    registryTypeId: null,
                                    versionId: null,
                                    itemSpecId: null,
                                    parents: [],
                                    typesToRoot: null
                                })
                            );
                            this.props.dispatch(registryDetailClear());
                        }}
                    />,
                    classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)
                )
            );
        };

        if (hasSpecification) {
            WebApi.specificationHasParty(descItem.descItemSpecId).then(open);
        } else {
            open();
        }
    };

    handleSelectModuleParty = ({onSelect, filterText, value}) => {
        console.log(value)
        const {partyList:{filter}} = this.props;
        this.props.dispatch(partyListFilter({...filter, text:filterText}));
        this.props.dispatch(partyDetailFetchIfNeeded(value ? value.id : null));
        this.props.dispatch(modalDialogShow(this, null, <PartySelectPage
            onSelect={(data) => {
                onSelect(data);
                this.props.dispatch(partyListFilter({text:null, type:null, itemSpecId: null}));
                this.props.dispatch(partyDetailClear());
            }}
        />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
    };

    canReplace() {
        const {userDetail} = this.props;
        const {selectedReplacementNode} = this.state;
        console.log(selectedReplacementNode);

        if (selectedReplacementNode) {
            return userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {
                    type: perms.REG_SCOPE_WR,
                    scopeId: selectedReplacementNode.scopeId
                });
            }
        return false;
    }

    render() {
        const {detail, fundTreeUsage, onReplace} = this.props;
        const {selectedReplacementNode, selectedNode} = this.state;
        return (
            <Modal.Body className="reg-usage-form">
                <h4>
                    {detail && detail.record}
                </h4>
                <label>
                    {i18n('registry.registryUsageCount')} {this.state.usageCount}
                </label>
                {fundTreeUsage &&
                <FundTreeUsage
                    handleOpenCloseNode={this.handleOpenCloseNode}
                    onNodeClick={this.handleNodeClick}
                    className="fund-tree-container-fixed"
                    cutLongLabels={true}
                    ref="treeUsage"
                    showCountStats={true}
                    {...fundTreeUsage}
                />}
                <ToggleContent withText text={this.props.replaceText}>
                    <Row>
                        <Col xs={10}>
                            {this.props.type === "registry" ? <RegistryField
                                value={this.state.selectedReplacementNode}
                                onChange={this.handleChoose}
                                onBlur={() => {
                                }}
                                detail
                                onSelectModule={this.handleSelectModuleRegistry}
                            /> :
                            <PartyField
                                value={this.state.selectedReplacementNode}
                                detail
                                onBlur={() => {
                                }}
                                onSelectModule={this.handleSelectModuleParty}
                                onChange={this.handleChoose}

                            />}
                        </Col>
                        <Col xs={2}>
                            <Button
                                onClick={() => onReplace(selectedReplacementNode, selectedNode)}
                                disabled={!this.canReplace()}
                            >
                                {i18n('registry.replace')}
                            </Button>
                        </Col>
                    </Row>
                </ToggleContent>
            </Modal.Body>
        );
    }

    componentWillUnmount() {
        this.props.dispatch({
            type: types.FUND_FUND_TREE_INVALIDATE,
        });

    }
}

export default connect((state, props) => {
    const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
    const partyList = storeFromArea(state, AREA_PARTY_LIST);

    return {
        fundTreeUsage: state.arrRegion.globalFundTree.fundTreeUsage,
        registryList,
        partyList,
        userDetail: state.userDetail
    };
})(RegistryUsageForm);
