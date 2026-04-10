import { useSelector } from 'react-redux';
import { i18n, Icon, RibbonGroup } from 'components/shared';
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
        dispatch(modalDialogShow(this, i18n('arr.history.title'), form, 'dialog-lg'));
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
                confirmMessage={i18n('arr.daos.fund.sync.confirm-message')}
                submittingMessage={i18n('arr.daos.fund.sync.submitting-message')}
                submitTitle={i18n('global.action.run')}
                onSubmit={async () => {
                    const result = await WebApi.syncDaosByFund(versionId);
                    dispatch(modalDialogHide());
                    return result;
                }}
            />
        );
        dispatch(modalDialogShow(this, i18n('arr.daos.fund.sync.title'), confirmForm));
    };

    const canCreateIssue = () => {
        return userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, { type: perms.FUND_ISSUE_ADMIN, fundId: activeFund.id }) ||
            userDetail.permissionsMap?.[perms.FUND_ISSUE_LIST_WR]?.issueListIds.length > 0;
    }

    const createIssue = (nodeId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                nodeId != null ? i18n('arr.issues.add.node.title') : i18n('arr.issues.add.arr.title'),
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
            if (userDetail.hasOne(perms.FUND_BA_ALL, { type: perms.FUND_BA, fundId: activeFund.id })) {
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
                <Button key="next-issue" onClick={handleIssuePrevious}>
                    <Icon glyph="fa-arrow-left" />
                    <span className="btnText">{i18n('ribbon.action.arr.issue.previous')}</span>
                </Button>,
                <Button key="previous-issue" onClick={handleIssueNext}>
                    <Icon glyph="fa-arrow-right" />
                    <span className="btnText">{i18n('ribbon.action.arr.issue.next')}</span>
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
                        <span className="btnText">{i18n('ribbon.action.arr.issue.add')}</span>
                    </span>
                }
                key="add-issue"
                id="add-issue"
            >
                <Dropdown.Item eventKey="1" onClick={createIssueFund}>
                    {i18n('arr.issues.add.arr')}
                </Dropdown.Item>
                <Dropdown.Item
                    eventKey="2"
                    disabled={subNodeId === null}
                    onClick={subNodeId !== null ? createIssueNode : null}
                >
                    {i18n('arr.issues.add.node')}
                </Dropdown.Item>
            </DropdownButton>,
        );
    }
    let altSection: React.ReactNode;

    altActions.push(
        <Button key="search-fa" onClick={showSearchModal}>
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
