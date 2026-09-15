import { useSelector } from 'react-redux';
import { Icon, RibbonGroup } from 'components/shared';
import { FormattedMessage, useIntl } from 'react-intl';
import { arrPageMessages } from './messages';
import { Button } from '../../components/ui';
import * as perms from '../../actions/user/Permission';
import { Dropdown, DropdownButton } from 'react-bootstrap';
import { Ribbon } from '../../components';
import ConfirmForm from '../../components/shared/form/ConfirmForm';
import { WebApi } from '../../actions/WebApi';
import { modalDialogHide, modalDialogShow } from '../../actions/global/modalDialog';
import ArrHistoryForm from "../../components/arr/ArrHistoryForm";
import IssueForm from '../../components/form/IssueForm';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as issuesActions from '../../actions/arr/issues';
import { nodeWithIssueByFundVersion } from '../../actions/arr/issues';
import { getFundVersion } from '../../constants';
import { useThunkDispatch } from 'utils/hooks';
import { AppState, UserDetail, Node } from 'typings/store';
import { IssueVO } from 'types';
import { useSearchFundsModal } from 'components/shared/dialog/FluentModalDialog';
import { globalMessages } from 'components/shared/lang/messages';

interface Props {
    handleChangeFundSettings: () => void;
    handleChangeFundTemplateSettings: () => void;
    handleChangeSyncTemplateSettings: () => void;
    handleErrorPrevious: () => void;
    handleErrorNext: () => void;
    handleOpenFundActionForm: () => void;
    handleFundsSearchForm: () => void;
    userDetail: UserDetail;
    readMode: boolean;
    selectedSubNodeId: number;
}

export default function ArrPageRibbonFn({
    handleChangeFundSettings,
    handleChangeFundTemplateSettings,
    handleChangeSyncTemplateSettings,
    handleErrorPrevious,
    handleErrorNext,
    handleOpenFundActionForm,
    userDetail,
    readMode,
    selectedSubNodeId,
}: Props) {
    const activeFund = useSelector(({ arrRegion }: AppState) => arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null);
    const issueProtocol = useSelector((state: AppState) => storeFromArea(state, issuesActions.AREA_PROTOCOL));
    const issueTypes = useSelector((state: AppState) => state.refTables.issueTypes);

    const intl = useIntl();
    const dispatch = useThunkDispatch();
    const showSearchModal = useSearchFundsModal();

    const handleShowFundHistory = (versionId: number, locked: boolean) => {
        const form = (
            <ArrHistoryForm
                versionId={versionId}
                locked={locked}
                onDeleteChanges={handleDeleteChanges}
            />
        );
        dispatch(modalDialogShow(this, intl.formatMessage(arrPageMessages.historyTitle), form, 'dialog-lg'));
    };

    const handleDeleteChanges = (nodeId: number, fromChangeId: number, toChangeId: number) => {
        const versionId = activeFund?.versionId;
        return WebApi.revertChanges(versionId, nodeId, fromChangeId, toChangeId)
    };

    /**
     * Zobrazení formuláře pro synchronizaci DAOS pro celé AS.
     *
     * @param versionId verze AS
     */
    const handleShowSyncDaosByFund = async (versionId: number) => {
        const confirmForm = (
            <ConfirmForm
                confirmMessage={<FormattedMessage {...arrPageMessages.daosFundSyncConfirmMessage} />}
                submittingMessage={intl.formatMessage(arrPageMessages.daosFundSyncSubmittingMessage)}
                submitTitle={<FormattedMessage {...globalMessages.run} />}
                onSubmit={async () => {
                    const result = await WebApi.syncDaosByFund(versionId);
                    dispatch(modalDialogHide());
                    return result;
                }}
            />
        );
        dispatch(modalDialogShow(this, intl.formatMessage(arrPageMessages.daosFundSyncTitle), confirmForm));
    };

    const canCreateIssue = () => {
        return userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, { type: perms.FUND_ISSUE_ADMIN, fundId: activeFund.id }) ||
            userDetail.permissionsMap?.[perms.FUND_ISSUE_LIST_WR]?.issueListIds.length > 0;
    }

    const createIssue = (nodeId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                nodeId != null ? intl.formatMessage(arrPageMessages.issuesAddNodeTitle) : intl.formatMessage(arrPageMessages.issuesAddArrTitle),
                <IssueForm
                    initialValues={{
                        issueListId: issueProtocol.id,
                        issueTypeId: issueTypes?.data?.[0].id,
                    }}
                    onSubmit={(data: IssueVO) =>
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

    const createIssueFund = () => {
        createIssue();
    };

    const createIssueNode = () => {
        if (activeFund?.nodes?.activeIndex !== null) {
            const node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
            if (node) {
                createIssue(node.selectedSubNodeId);
            }
        }
    };

    const handleIssuePrevious = () => {
        handleIssue(-1);
    };

    const handleIssueNext = () => {
        handleIssue(1);
    };

    const handleIssue = (direction: -1 | 1) => {
        if (activeFund) {
            const nodeIndex = activeFund.nodes.activeIndex;
            if (nodeIndex !== null) {
                const activeNode = activeFund.nodes.nodes[nodeIndex];
                dispatch(nodeWithIssueByFundVersion(activeFund, activeNode.selectedSubNodeId, direction));
            }
        }
    };

    const altActions = [];

    const itemActions = [];

    if (activeFund) {
        altActions.push(
            <Button key="fund-settings" onClick={handleChangeFundSettings} variant={'default'}>
                <Icon glyph="fa-wrench" />
                <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrFundSettingsUi} />}</span>
            </Button>,
        );

        altActions.push(
            <Button key="fund-templates" onClick={handleChangeFundTemplateSettings} variant={'default'}>
                <Icon glyph="fa-wrench" />
                <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrFundSettingsTemplate} />}</span>
            </Button>,
        );

        altActions.push(
            <Button
                key="sync-templates"
                onClick={handleChangeSyncTemplateSettings.bind(this, activeFund.id)}
                variant={'default'}
            >
                <Icon glyph="fa-wrench" />
                <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrFundSettingsRefTemplate} />}</span>
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
                { type: perms.FUND_ARR, fundId: activeFund.id },
            )
        ) {
            altActions.push(
                <Button
                    onClick={() => handleShowFundHistory(activeFund.versionId, readMode)}
                    key="show-fund-history"
                    variant={'default'}
                >
                    <Icon glyph="fa-clock-o" />
                    <div>
                        <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionShowFundHistory} />}</span>
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
                { type: perms.FUND_ARR, fundId: activeFund.id },
            )
        ) {
            altActions.push(
                <Button
                    onClick={() => handleShowSyncDaosByFund(activeFund.versionId)}
                    key="show-sync-daos-by-fund"
                    variant={'default'}
                >
                    <Icon glyph="fa-camera" />
                    <div>
                        <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionSyncDaosByFund} />}</span>
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
                    <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrValidationErrorPrevious} />}</span>
                </Button>,
                <Button key="previous-error" onClick={handleErrorNext} variant={'default'}>
                    <Icon glyph="fa-arrow-right" />
                    <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrValidationErrorNext} />}</span>
                </Button>,
            );
            if (userDetail.hasOne(perms.FUND_BA_ALL, { type: perms.FUND_BA, fundId: activeFund.id })) {
                itemActions.push(
                    <Button
                        disabled={readMode}
                        key="prepareFundAction"
                        onClick={handleOpenFundActionForm}
                        variant={'default'}
                    >
                        <Icon glyph="fa-calculator" />
                        <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrFundNewFundAction} />}</span>
                    </Button>,
                );
            }
        }

        if (selectedSubNodeId !== null) {
            subNodeId = selectedSubNodeId;
            itemActions.push(
                <Button key="next-issue" onClick={handleIssuePrevious}>
                    <Icon glyph="fa-arrow-left" />
                    <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrIssuePrevious} />}</span>
                </Button>,
                <Button key="previous-issue" onClick={handleIssueNext}>
                    <Icon glyph="fa-arrow-right" />
                    <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrIssueNext} />}</span>
                </Button>,
            );
        }

        itemActions.push(
            <DropdownButton
                disabled={!canCreateIssue()}
                variant="default"
                title={
                    <span>
                        <Icon glyph="fa-commenting" />
                        <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrIssueAdd} />}</span>
                    </span>
                }
                key="add-issue"
                id="add-issue"
            >
                <Dropdown.Item eventKey="1" onClick={createIssueFund}>
                    {<FormattedMessage {...arrPageMessages.issuesAddArr} />}
                </Dropdown.Item>
                <Dropdown.Item
                    eventKey="2"
                    disabled={subNodeId === null}
                    onClick={subNodeId !== null ? createIssueNode : null}
                >
                    {<FormattedMessage {...arrPageMessages.issuesAddNode} />}
                </Dropdown.Item>
            </DropdownButton>,
        );
    }
    let altSection: React.ReactNode;

    altActions.push(
        <Button key="search-fa" onClick={showSearchModal}>
            <Icon glyph="fa-search" />
            <div>
                <span className="btnText">{<FormattedMessage {...arrPageMessages.ribbonActionArrFundSearch} />}</span>
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

    let itemSection: React.ReactNode;
    if (itemActions.length > 0) {
        itemSection = (
            <RibbonGroup key="item" className="small">
                {itemActions}
            </RibbonGroup>
        );
    }

    return <Ribbon
        arr
        subMenu
        versionId={getFundVersion(activeFund)}
        fundId={activeFund?.id}
        altSection={altSection}
        itemSection={itemSection}
    />;
}
