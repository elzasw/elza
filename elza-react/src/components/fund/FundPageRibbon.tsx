import { Icon, Ribbon, i18n } from 'components';
import { Button } from 'components/ui';
import * as perms from 'actions/user/Permission';
import { RibbonGroup } from 'components/shared';
import { JSX, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { useSearchFundsModal } from 'components/shared/dialog/FluentModalDialog';
import { PublicationSystemsDialog } from 'components/arr/publication/PublicationSystemsDialog';
import { FormattedMessage } from 'react-intl';

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
    const [publicationSystemsOpen, setPublicationSystemsOpen] = useState(false);

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

    if (userDetail.hasOne(perms.FUND_ADMIN)) {
        altActions.push(
            <Button key="publication-systems" onClick={() => setPublicationSystemsOpen(true)}>
                <Icon glyph="fa-newspaper-o" />
                <div>
                    <span className="btnText">
                        <FormattedMessage id="ribbon.action.arr.fund.publicationSystems" defaultMessage="Typy publikací" />
                    </span>
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

    return <>
        <Ribbon altSection={altSection} />
        <PublicationSystemsDialog
            open={publicationSystemsOpen}
            onClose={() => setPublicationSystemsOpen(false)}
        />
    </>;
}
