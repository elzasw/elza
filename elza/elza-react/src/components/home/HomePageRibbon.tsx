import Ribbon from "components/page/Ribbon";
import { i18n, Icon, RibbonGroup } from "components/shared";
import { Button } from "components/ui";
import * as perms from 'actions/user/Permission.jsx';
import { useSelector } from "react-redux";
import { AppState } from "typings/store";
import { useSearchFundsModal } from "components/shared/dialog/FluentModalDialog";

interface Props {
  onAddFund: () => void;
}

export function HomePageRibbon({ onAddFund }: Props) {
  const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
  const altActions = [];

  const showSearchModal = useSearchFundsModal();

  if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
    altActions.push(
      <Button key="add-fa" onClick={onAddFund}>
        <Icon glyph="fa-plus-circle" />
        <div>
          <span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span>
        </div>
      </Button>,
    );
  }

  if (userDetail.hasOne(perms.FUND_RD, perms.FUND_RD_ALL)) {
    altActions.push(
      <Button key="search-fa" onClick={showSearchModal}>
        <Icon glyph="fa-search" />
        <div>
          <span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span>
        </div>
      </Button>,
    );
  }

  const altSection = altActions.length > 0 ? (
    <RibbonGroup className="small" key="ribbon-group-home">
      {altActions}
    </RibbonGroup>
  ) : undefined;

  return <Ribbon altSection={altSection} />;
}
