import { Icon, Ribbon, i18n } from 'components';
import { Button } from 'components/ui';
import * as perms from 'actions/user/Permission';
import { RibbonGroup } from 'components/shared';
import { JSX } from 'react/jsx-runtime';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { useSearchFundsModal } from 'components/shared/dialog/FluentModalDialog';

interface Props {
    onAddFund: () => void;
    onImport: () => void;
}

export function FundPageRibbon({
    onAddFund,
    onImport,
}: Props) {
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);

    const showSearchModal = useSearchFundsModal();

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
        <Button key="search-fa" onClick={showSearchModal}>
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

    let altSection: React.ReactNode[];
    if (altActions.length > 0) {
        altSection = [
            <RibbonGroup key="alt-actions" className="small">
                {altActions}
            </RibbonGroup>
        ];
    }

    return <Ribbon
        altSection={altSection}
    />;
}
