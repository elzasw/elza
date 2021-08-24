import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, RibbonGroup} from 'components/shared';
import {Button} from '../../components/ui';
import * as perms from '../../actions/user/Permission';
import {Dropdown, DropdownButton} from 'react-bootstrap';
import {Ribbon} from '../../components';
import {propsEquals} from '../../components/Utils';
import ConfirmForm from '../../components/shared/form/ConfirmForm';
import {WebApi} from '../../actions/WebApi';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import ArrHistoryForm from "../../components/arr/ArrHistoryForm";
import IssueForm from '../../components/form/IssueForm';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as issuesActions from '../../actions/arr/issues';
import {nodeWithIssueByFundVersion} from '../../actions/arr/issues';

const clientLog = window.clientLog !== undefined && window.clientLog;

class ArrPageRibbon extends AbstractReactComponent {
    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        return !propsEquals(this.props, nextProps, null, clientLog);
    }

    /**
     * Zobrazení formuláře historie.
     * @param versionId verze AS
     * @param locked
     */
    handleShowFundHistory = (versionId, locked) => {
        const form = (
            <ArrHistoryForm versionId={versionId} locked={locked} onDeleteChanges={this.handleDeleteChanges} />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.history.title'), form, 'dialog-lg'));
    };

    handleDeleteChanges = (nodeId, fromChangeId, toChangeId) => {
        const { activeFund } = this.props;
        const versionId = activeFund?.versionId;
        WebApi.revertChanges(versionId, nodeId, fromChangeId, toChangeId).then(() => {
            this.props.dispatch(modalDialogHide());
        });
    };

    /**
     * Zobrazení formuláře pro synchronizaci DAOS pro celé AS.
     *
     * @param versionId verze AS
     */
    handleShowSyncDaosByFund = versionId => {
        const confirmForm = (
            <ConfirmForm
                confirmMessage={i18n('arr.daos.fund.sync.confirm-message')}
                submittingMessage={i18n('arr.daos.fund.sync.submitting-message')}
                submitTitle={i18n('global.action.run')}
                onSubmit={() => {
                    return WebApi.syncDaosByFund(versionId);
                }}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                }}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.daos.fund.sync.title'), confirmForm));
    };

    render() {
        const {
            activeFund,
            selectedSubNodeId,
            readMode,
            userDetail,
            handleChangeFundSettings,
            handleChangeFundTemplateSettings,
            handleChangeSyncTemplateSettings,
            handleErrorPrevious,
            handleErrorNext,
            handleOpenFundActionForm,
        } = this.props;

        const altActions = [];

        const itemActions = [];

        if (activeFund) {
            altActions.push(
                <Button key="fund-settings" onClick={handleChangeFundSettings} variant={'default'}>
                    <Icon glyph="fa-wrench" />
                    <span className="btnText">{i18n('ribbon.action.arr.fund.settings.ui')}</span>
                </Button>,
            );

            altActions.push(
                <Button key="fund-templates" onClick={handleChangeFundTemplateSettings} variant={'default'}>
                    <Icon glyph="fa-wrench" />
                    <span className="btnText">{i18n('ribbon.action.arr.fund.settings.template')}</span>
                </Button>,
            );

            altActions.push(
                <Button
                    key="sync-templates"
                    onClick={handleChangeSyncTemplateSettings.bind(this, activeFund.id)}
                    variant={'default'}
                >
                    <Icon glyph="fa-wrench" />
                    <span className="btnText">{i18n('ribbon.action.arr.fund.settings.refTemplate')}</span>
                </Button>,
            );

            // Zobrazení historie změn
            if (
                userDetail.hasOne(
                    perms.FUND_ADMIN,
                    {
                        type: perms.FUND_VER_WR,
                        fundId: activeFund.id,
                    },
                    perms.FUND_ARR_ALL,
                    {type: perms.FUND_ARR, fundId: activeFund.id},
                )
            ) {
                altActions.push(
                    <Button
                        onClick={() => this.handleShowFundHistory(activeFund.versionId, readMode)}
                        key="show-fund-history"
                        variant={'default'}
                    >
                        <Icon glyph="fa-clock-o" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.showFundHistory')}</span>
                        </div>
                    </Button>,
                );
            }

            if (
                userDetail.hasOne(
                    perms.FUND_ADMIN,
                    {
                        type: perms.FUND_VER_WR,
                        fundId: activeFund.id,
                    },
                    perms.FUND_ARR_ALL,
                    {type: perms.FUND_ARR, fundId: activeFund.id},
                )
            ) {
                altActions.push(
                    <Button
                        onClick={() => this.handleShowSyncDaosByFund(activeFund.versionId)}
                        key="show-sync-daos-by-fund"
                        variant={'default'}
                    >
                        <Icon glyph="fa-camera" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.syncDaosByFund')}</span>
                        </div>
                    </Button>,
                );
            }

            let subNodeId = null;
            if (selectedSubNodeId !== null) {
                subNodeId = selectedSubNodeId;
                itemActions.push(
                    <Button key="next-error" onClick={handleErrorPrevious} variant={'default'}>
                        <Icon glyph="fa-arrow-left" />
                        <span className="btnText">{i18n('ribbon.action.arr.validation.error.previous')}</span>
                    </Button>,
                    <Button key="previous-error" onClick={handleErrorNext} variant={'default'}>
                        <Icon glyph="fa-arrow-right" />
                        <span className="btnText">{i18n('ribbon.action.arr.validation.error.next')}</span>
                    </Button>,
                );
                if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: activeFund.id})) {
                    itemActions.push(
                        <Button
                            disabled={readMode}
                            key="prepareFundAction"
                            onClick={handleOpenFundActionForm}
                            variant={'default'}
                        >
                            <Icon glyph="fa-calculator" />
                            <span className="btnText">{i18n('ribbon.action.arr.fund.newFundAction')}</span>
                        </Button>,
                    );
                }
            }

            if (selectedSubNodeId !== null) {
                subNodeId = selectedSubNodeId;
                itemActions.push(
                    <Button key="next-issue" onClick={this.handleIssuePrevious}>
                        <Icon glyph="fa-arrow-left" />
                        <span className="btnText">{i18n('ribbon.action.arr.issue.previous')}</span>
                    </Button>,
                    <Button key="previous-issue" onClick={this.handleIssueNext}>
                        <Icon glyph="fa-arrow-right" />
                        <span className="btnText">{i18n('ribbon.action.arr.issue.next')}</span>
                    </Button>,
                );
            }

            itemActions.push(
                <DropdownButton
                    disabled={!this.canCreateIssue()}
                    variant="default"
                    title={
                        <span>
                            <Icon glyph="fa-commenting" />
                            <span className="btnText">{i18n('ribbon.action.arr.issue.add')}</span>
                        </span>
                    }
                    key="add-issue"
                    id="add-issue"
                >
                    <Dropdown.Item eventKey="1" onClick={this.createIssueFund}>
                        {i18n('arr.issues.add.arr')}
                    </Dropdown.Item>
                    <Dropdown.Item
                        eventKey="2"
                        disabled={subNodeId === null}
                        onClick={subNodeId !== null ? this.createIssueNode : null}
                    >
                        {i18n('arr.issues.add.node')}
                    </Dropdown.Item>
                </DropdownButton>,
            );
        }
        let altSection;

        altActions.push(
            <Button key="search-fa" onClick={this.props.handleFundsSearchForm}>
                <Icon glyph="fa-search" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span>
                </div>
            </Button>,
        );

        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon arr subMenu fundId={activeFund?.id} altSection={altSection} itemSection={itemSection} />;
    }

    canCreateIssue = () => {
        const {activeFund, userDetail} = this.props;

        return userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, {type: perms.FUND_ISSUE_ADMIN, fundId: activeFund.id}) ||
            userDetail.permissionsMap?.[perms.FUND_ISSUE_LIST_WR]?.issueListIds.length > 0;
    }

    createIssue = (nodeId) => {
        const {dispatch, issueProtocol, issueTypes} = this.props;

        dispatch(
            modalDialogShow(
                this,
                nodeId != null ? i18n('arr.issues.add.node.title') : i18n('arr.issues.add.arr.title'),
                <IssueForm
                    initialValues={{
                        issueListId: issueProtocol.id,
                        issueTypeId: issueTypes?.data?.[0].id,
                    }}
                    onSubmit={data =>
                        WebApi.addIssue({
                            ...data,
                            nodeId,
                        })
                    }
                    onSubmitSuccess={data => {
                        dispatch(issuesActions.list.invalidate(data.issueListId));
                        dispatch(issuesActions.detail.invalidate(data.id));
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    }

    createIssueFund = () => {
        this.createIssue();
    };

    createIssueNode = () => {
        const { activeFund } = this.props;

        let node;
        if (activeFund?.nodes?.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        this.createIssue(node.selectedSubNodeId);
    };

    handleIssuePrevious = () => {
        this.handleIssue(-1);
    };

    handleIssueNext = () => {
        this.handleIssue(1);
    };

    handleIssue = direction => {
        const {activeFund, dispatch} = this.props;
        if (activeFund) {
            const nodeIndex = activeFund.nodes.activeIndex;
            if (nodeIndex !== null) {
                const activeNode = activeFund.nodes.nodes[nodeIndex];
                dispatch(nodeWithIssueByFundVersion(activeFund, activeNode.selectedSubNodeId, direction));
            }
        }
    };
}

function mapStateToProps(state) {
    const {arrRegion, refTables} = state;
    return {
        activeFund: arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null,
        issueProtocol: storeFromArea(state, issuesActions.AREA_PROTOCOL),
        issueTypes: refTables.issueTypes,
    }
}

// export default ArrPageRibbon;
export default connect(mapStateToProps, null, null, {forwardRef: true})(ArrPageRibbon);
