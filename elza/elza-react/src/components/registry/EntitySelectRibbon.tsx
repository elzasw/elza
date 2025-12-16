import { useSelector } from "react-redux";
import { AppState } from "typings/store";
import * as perms from 'actions/user/Permission.jsx';
import { Button } from "components/ui";
import { i18n, Icon, Ribbon } from "components";
import { RibbonGroup } from "components/shared";

interface Props {
  customRibbon: { altActions: JSX.Element[], itemActions: JSX.Element[], primarySection: JSX.Element };
  module: boolean;
  onAddRegistry: () => void;
}

export function EntitySelectRibbon({
  module,
  customRibbon,
  onAddRegistry,
}: Props) {

  const userDetail = useSelector(({ userDetail }: AppState) => userDetail);

  const parts = module && customRibbon ? customRibbon : { altActions: [], itemActions: [], primarySection: null };

  const altActions = [...parts.altActions];

  if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
    altActions.push(
      <Button key="addRegistry" onClick={onAddRegistry}>
        <Icon glyph="fa-plus-circle" />
        <div>
          <span className="btnText">{i18n('registry.addNewRegistry')}</span>
        </div>
      </Button>,
    );
  }

  const itemActions = [...parts.itemActions];

  const altSection = altActions.length > 0 ? (
    <>
      {altActions.length > 0 &&
        <RibbonGroup className="small" >
          {altActions}
        </RibbonGroup>}
      {itemActions.length > 0 &&
        <RibbonGroup className="small" >
          {itemActions}
        </RibbonGroup>}
    </>
  ) : undefined;

  return <Ribbon
    primarySection={parts.primarySection}
    altSection={altSection}
    showUser={false}
  />;
}
