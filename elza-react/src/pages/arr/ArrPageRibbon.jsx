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

class ArrPageRibbon extends AbstractReactComponent {
    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        return !propsEquals(this.props, nextProps, null, true);
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
        const activeFund = this.getActiveFund(this.props);
        const versionId = activeFund.versionId;
        WebApi.revertChanges(versionId, nodeId, fromChangeId, toChangeId).then(() => {
            this.props.dispatch(modalDialogHide());
        });
    };

    getActiveFund(props) {
        const arrRegion = props.arrRegion;
        return arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

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
            activeFundId,
            activeFundVersionId,
            selectedSubNodeId,
            readMode,
            arrRegionActiveIndex,
            userDetail,
            handleChangeFundSettings,
            handleChangeFundTemplateSettings,
            handleChangeSyncTemplateSettings,
            handleErrorPrevious,
            handleErrorNext,
            handleOpenFundActionForm,
            issueProtocol,
        } = this.props;

        const altActions = [];

        const itemActions = [];

        const indexFund = arrRegionActiveIndex;
        if (indexFund !== null) {
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
                    onClick={handleChangeSyncTemplateSettings.bind(this, activeFundId)}
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
                        fundId: activeFundId,
                    },
                    perms.FUND_ARR_ALL,
                    {type: perms.FUND_ARR, fundId: activeFundId},
                )
            ) {
                altActions.push(
                    <Button
                        onClick={() => this.handleShowFundHistory(activeFundVersionId, readMode)}
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
                        fundId: activeFundId,
                    },
                    perms.FUND_ARR_ALL,
                    {type: perms.FUND_ARR, fundId: activeFundId},
                )
            ) {
                altActions.push(
                    <Button
                        onClick={() => this.handleShowSyncDaosByFund(activeFundVersionId)}
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
                if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: activeFundId})) {
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

            const isProtocolLoaded =
                !issueProtocol.isFetching && issueProtocol.data && activeFundId === issueProtocol.data.fundId;

            const haveProtocolPermissionToWrite =
                isProtocolLoaded &&
                (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL) ||
                    (userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR] &&
                        userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds &&
                        userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds.indexOf(
                            issueProtocol.data.id,
                        ) !== -1));
            // const haveProtocolPermissionToRead =
            //     haveProtocolPermissionToWrite || (isProtocolLoaded &&
            //     userDetail.permissionsMap[perms.FUND_ISSUE_LIST_RD] &&
            //     userDetail.permissionsMap[perms.FUND_ISSUE_LIST_RD].issueListIds &&
            //     userDetail.permissionsMap[perms.FUND_ISSUE_LIST_RD].issueListIds.indexOf(issueProtocol.data.id) !== -1);

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
                    variant="default"
                    disabled={!haveProtocolPermissionToWrite}
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
            <Button key="search-fa" onClick={this.handleFundsSearchForm}>
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

        return <Ribbon arr subMenu fundId={activeFundId} altSection={altSection} itemSection={itemSection} />;
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion} = state;
    return {
        arrRegion
    }
}

// export default ArrPageRibbon;
export default connect(mapStateToProps, null, null, {forwardRef: true})(ArrPageRibbon);
