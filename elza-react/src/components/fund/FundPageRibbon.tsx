import { /* FundForm, */ Icon, Ribbon, i18n } from 'components';
import { Button } from 'components/ui';
import * as perms from 'actions/user/Permission';
import { RibbonGroup } from 'components/shared';
import { JSX } from 'react/jsx-runtime';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { useSearchFundsModal } from 'components/shared/dialog/FluentModalDialog';
// import { modalDialogShow } from 'actions/global/modalDialog';
// import { WebApi } from 'actions';
// import { useThunkDispatch } from 'utils/hooks';
// import { createFund } from 'actions/arr/fund';

interface Props {
    onAddFund: () => void;
    onImport: () => void;
}

export function FundPageRibbon({
    onAddFund,
    onImport,
}: Props) {
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    // const dispatch = useThunkDispatch();

    const showModal = useSearchFundsModal();

    // async function handleAddFund() {
    //     const initData = {};
    //     if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
    //         initData.fundAdmins = [{ id: 'default', user: userDetail }];
    //     }
    //     const scopes = await WebApi.getAllScopes();
    //     dispatch(modalDialogShow(
    //         this,
    //         i18n('arr.fund.title.add'),
    //         <FundForm
    //             create={true}
    //             initialValues={initData}
    //             scopeList={scopes}
    //             onSubmitForm={data => {
    //                 return dispatch(createFund(data));
    //             }}
    //         />,
    //     ),
    //     );
    // }

    async function handleTestModal() {
        const { result } = await showModal()
        console.log("#ff - modal result", result);
    }

    const altActions = [];
    if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
        altActions.push(
            <Button key="add-fa" onClick={onAddFund}>
                <Icon glyph="fa-plus-circle" />
                <div>
                    <span className="btnText important">{i18n('ribbon.action.arr.fund.add')}</span>
                </div>
            </Button>,
        );
    }

    altActions.push(
        <Button key="search-fa" onClick={handleTestModal}>
            <Icon glyph="fa-search" />
            <div>
                <span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span>
            </div>
        </Button>,
    );

    if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
        altActions.push(
            <Button key="fa-import" onClick={onImport}>
                <Icon glyph="fa-upload" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span>
                </div>
            </Button>,
        );
    }


    // const itemActions = [];
    // if (fundRegion.fundDetail.id !== null && !fundRegion.fundDetail.fetching && fundRegion.fundDetail.fetched) {
    //     if (userDetail.hasOne(perms.FUND_ADMIN, { type: perms.FUND_VER_WR, fundId: fundRegion.fundDetail.id })) {
    //         itemActions.push(
    //             <Button key="edit-version" onClick={onEditFundVersion}>
    //                 <Icon glyph="fa-pencil" />
    //                 <div>
    //                     <span className="btnText">{i18n('ribbon.action.arr.fund.update')}</span>
    //                 </div>
    //             </Button>,
    //             <Button key="rule-set-version" onClick={onRuleSetUpdateFundVersion}>
    //                 <Icon glyph="fa-calendar-check-o" />
    //                 <div>
    //                     <span className="btnText">{i18n('ribbon.action.arr.fund.ruleSet')}</span>
    //                 </div>
    //             </Button>,
    //             <Button key="approve-version" onClick={onApproveFundVersion}>
    //                 <Icon glyph="fa-code-fork" />
    //                 <div>
    //                     <span className="btnText">{i18n('ribbon.action.arr.fund.approve')}</span>
    //                 </div>
    //             </Button>,
    //         );
    //     }
    //     if (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, { type: perms.FUND_ISSUE_ADMIN, fundId: fundRegion.fundDetail.id })) {
    //         itemActions.push(
    //             <Button key="fa-lecturing" onClick={onIssuesSettings}>
    //                 <Icon glyph="fa-commenting" />
    //                 <div>
    //                     <span className="btnText">{i18n('arr.issues.settings.title')}</span>
    //                 </div>
    //             </Button>,
    //         );
    //     }
    //     if (userDetail.hasOne(perms.FUND_ADMIN)) {
    //         itemActions.push(
    //             <Button key="fa-delete" onClick={onDeleteFund}>
    //                 <Icon glyph="fa-trash" />
    //                 <div>
    //                     <span className="btnText">{i18n('arr.fund.action.delete')}</span>
    //                 </div>
    //             </Button>,
    //         );
    //         itemActions.push(
    //             <Button key="fa-deletehistory" onClick={onDeleteFundHistory}>
    //                 <Icon glyph="fa-times-circle-o" />
    //                 <div>
    //                     <span className="btnText">{i18n('arr.fund.action.deletehistory')}</span>
    //                 </div>
    //             </Button>,
    //         );
    //     }
    //     if (userDetail.hasOne(perms.FUND_EXPORT_ALL, { type: perms.FUND_EXPORT, fundId: fundRegion.fundDetail.id })) {
    //         itemActions.push(
    //             <Button key="fa-export" onClick={onExport}>
    //                 <Icon glyph="fa-download" />
    //                 <div>
    //                     <span className="btnText">{i18n('ribbon.action.arr.fund.export')}</span>
    //                 </div>
    //             </Button>,
    //         );
    //     }
    // }
    //
    let altSection: JSX.Element[];
    if (altActions.length > 0) {
        altSection = [
            <RibbonGroup key="alt-actions" className="small">
                {altActions}
            </RibbonGroup>
        ];
    }

    // let itemSection: JSX.Element[];
    // if (itemActions.length > 0) {
    //     itemSection = [
    //         <RibbonGroup key="item-actions" className="small">
    //             {itemActions}
    //         </RibbonGroup>
    //     ];
    // }

    return <Ribbon
        altSection={altSection}
    // itemSection={itemSection}
    />;
}
