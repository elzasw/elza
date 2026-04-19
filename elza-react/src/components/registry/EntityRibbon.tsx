import { useSelector } from "react-redux";
import { AppState, RegistryDetail } from "typings/store";
import * as perms from 'actions/user/Permission.jsx';
import { Button } from "components/ui";
import { i18n, Icon, Ribbon } from "components";
import { AP_EXT_SYSTEM_TYPE } from "../../constants";
import { isMenuItemHidden } from "api/settings/utils";
import { MenuOptions } from "api/settings/MenuOption";
import { RibbonGroup } from "components/shared";
import { useDeletedEntityWindow } from "./deleted-entity/hooks";
import { useIntl } from "react-intl";
import { messages } from "./messages";

interface Props {
  customRibbon: { altActions: JSX.Element[], itemActions: JSX.Element[], primarySection: JSX.Element };
  module: boolean;
  revisionActive: boolean;
  select: boolean;
  onAddRegistry: () => void;
  onImportRegistry: () => void;
  onApExtSearch: () => void;
  onExtSyncs: () => void;
  onScopeManagement: () => void;
  onDeleteRegistry: () => void;
  onRegistryShowUsage: (detail: RegistryDetail) => void;
  onRemoveDuplicity: (detail: RegistryDetail) => void;
  onShowApHistory: () => void;
  onChangeApState: () => void;
  onCopyAp: () => void;
  onDeleteRevision: () => void;
  onChangeRevisionState: () => void;
  onMergeRevision: () => void;
  onCreateRevision: () => void;
  onRestoreEntity: () => void;
}

export function EntityRibbon({
  module,
  customRibbon,
  select,
  revisionActive,
  onAddRegistry,
  onImportRegistry,
  onApExtSearch,
  onExtSyncs,
  onScopeManagement,
  onDeleteRegistry,
  onRegistryShowUsage,
  onRemoveDuplicity,
  onShowApHistory,
  onChangeApState,
  onCopyAp,
  onDeleteRevision,
  onChangeRevisionState,
  onMergeRevision,
  onCreateRevision,
  onRestoreEntity
}: Props) {

  const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
  const extSystems = useSelector(({ app }: AppState) => app.apExtSystemList.rows);
  const registryDetail = useSelector(({ app }: AppState) => app.registryDetail);
  const { data, id } = registryDetail;

  const showDeleteEntityWindow = useDeletedEntityWindow();
  const { formatMessage } = useIntl();


  function canDeleteRegistry() {
    // We can delete item if has id, data and if it is not part of the external
    // system with CAM_COMPLETE type
    const externalSystemId = data?.bindings?.[0]?.externalSystemId;
    const externalSystem = extSystems?.find((externalSystem) => externalSystem.id === externalSystemId);
    const isCompleteExternalSystem = externalSystem?.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE;
    return id && data && !isCompleteExternalSystem;
  }

  const parts = module && customRibbon ? customRibbon : { altActions: [], itemActions: [], primarySection: null };
  const hasRevision = data?.revStateApproval != null;

  const completeExternalSystems = extSystems?.filter((extSystem) => extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE);
  const hasOnlyCompleteExternalSystems = completeExternalSystems?.length === extSystems?.length;

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
    if (!select) {
      altActions.push(
        <Button key="registryImport" onClick={onImportRegistry}>
          <Icon glyph="fa-file" />
          <div>
            <span className="btnText">{i18n('ribbon.action.registry.import')}</span>
          </div>
        </Button>,
      );

      if (extSystems && extSystems.length > 0 && !hasOnlyCompleteExternalSystems) {
        altActions.push(
          <Button key="ap-ext-search" onClick={onApExtSearch}>
            <Icon glyph="fa-cloud-download" />
            <div>
              <span className="btnText">{i18n('ribbon.action.ap.ext-search')}</span>
            </div>
          </Button>,
        );
      }
      if (extSystems && extSystems.length > 0 && !isMenuItemHidden(userDetail.settings, MenuOptions.RIBBON_AP_EXT_SYNCS_HIDDEN)) {
        altActions.push(
          <Button key="ext-syncs" onClick={onExtSyncs}>
            <Icon glyph="fa-gg" />
            <div>
              <span className="btnText">{i18n('ribbon.action.ap.ext-syncs')}</span>
            </div>
          </Button>,
        );
      }
      altActions.push(
        <Button key="invalidated-entities" onClick={showDeleteEntityWindow}>
          <Icon glyph="fa-trash" />
          <div>
            <span className="btnText">{formatMessage(messages.invalidatedEntities)}</span>
          </div>
        </Button>,
      );
    }
  }

  const itemActions = [...parts.itemActions];
  const revisionActions = [];
  const invalidItemActions = [];

  if (!select) {
    if (userDetail.hasOne(perms.ADMIN)) {
      altActions.push(
        <Button key="scopeManagement" onClick={onScopeManagement}>
          <Icon glyph="fa-wrench" />
          <div>
            <span className="btnText">{i18n('ribbon.action.registry.scope.manage')}</span>
          </div>
        </Button>,
      );
    }

    if (
      userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
        type: perms.AP_SCOPE_WR,
        scopeId: data ? data.scopeId : null,
      }) && canDeleteRegistry()
    ) {
      itemActions.push(
        <Button disabled={data.invalid} key="registryRemove" onClick={onDeleteRegistry}>
          <Icon glyph="fa-trash" />
          <div>
            <span className="btnText">{i18n('registry.deleteRegistry')}</span>
          </div>
        </Button>,
      );
    }

    if (
      userDetail.hasOne(
        perms.AP_SCOPE_RD_ALL,
        {
          type: perms.AP_SCOPE_RD,
          scopeId: data ? data.scopeId : null,
        }
      ) && userDetail.hasOne(
        perms.FUND_RD,
        perms.FUND_RD_ALL,
      )
    ) {
      itemActions.push(
        <Button key="registryShow" onClick={() => onRegistryShowUsage(registryDetail)}>
          <Icon glyph="fa-search" />
          <div>
            <span className="btnText">{i18n('registry.registryUsage')}</span>
          </div>
        </Button>,
      );
    }

    if (
      userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
        type: perms.AP_SCOPE_WR,
        scopeId: data ? data.scopeId : null,
      })
      && canDeleteRegistry()
      && !isMenuItemHidden(userDetail.settings, MenuOptions.RIBBON_AP_REMOVEDUPLICITY_HIDDEN)
    ) {
      itemActions.push(
        <Button key="deleteReplaceAccessPoint" onClick={() => onRemoveDuplicity(registryDetail)}>
          <Icon glyph="fa-ban" />
          <div>
            <span className="btnText">{i18n('accesspoint.removeDuplicity')}</span>
          </div>
        </Button>,
      );
    }

    if (id && data) {
      itemActions.push(
        <Button key="show-state-history" onClick={onShowApHistory}>
          <Icon glyph="fa-clock-o" />
          <div>
            <span className="btnText">{i18n('ap.stateHistory')}</span>
          </div>
        </Button>,
      );

      if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR,
        perms.AP_CONFIRM_ALL, perms.AP_CONFIRM,
        perms.AP_EDIT_CONFIRMED_ALL, perms.AP_EDIT_CONFIRMED
      )) {
        itemActions.push(
          <Button key="change-state" onClick={onChangeApState} disabled={hasRevision}>
            <Icon glyph="fa-pencil" />
            <div>
              <span className="btnText">{i18n('ap.changeState')}</span>
            </div>
          </Button>,
        );
      }

      if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
        itemActions.push(
          <Button key="push-ap-to-ext" onClick={onCopyAp}>
            <Icon glyph="fa-copy" />
            <div>
              <span className="btnText">{i18n("ap.copy.title")}</span>
            </div>
          </Button>,
        );
      }

      if (hasRevision) {
        revisionActions.push(
          <Button disabled={data.invalid || !revisionActive} key="revisionDelete" onClick={onDeleteRevision}>
            <Icon glyph="fa-undo" />
            <div>
              <span className="btnText">{i18n('registry.deleteRevision')}</span>
            </div>
          </Button>,
        );
        revisionActions.push(
          <Button disabled={data.invalid || !revisionActive} key="revisionChangeState" onClick={onChangeRevisionState}>
            <Icon glyph="fa-pencil" />
            <div>
              <span className="btnText">{i18n('registry.changeStateRevision')}</span>
            </div>
          </Button>,
        );
        revisionActions.push(
          <Button disabled={data.invalid || !revisionActive} key="revisionMerge" onClick={onMergeRevision}>
            <Icon glyph="fa-check" />
            <div>
              <span className="btnText">{i18n('registry.mergeRevision')}</span>
            </div>
          </Button>,
        );
      } else if (!data.invalid) {
        revisionActions.push(
          <Button disabled={data.invalid} key="revisionCreate" onClick={onCreateRevision}>
            <Icon glyph="fa-plus" />
            <div>
              <span className="btnText">{i18n('registry.createRevision')}</span>
            </div>
          </Button>,
        );
      }
      if (data.invalid) {
        invalidItemActions.push(
          <Button key="restoreEntity" onClick={onRestoreEntity}>
            <Icon glyph="fa-undo" />
            <div>
              <span className="btnText">{i18n('registry.restoreEntity')}</span>
            </div>
          </Button>,
        );
      }
    }
  }

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
      {revisionActions.length > 0 &&
        <RibbonGroup className="small" >
          {revisionActions}
        </RibbonGroup>}
      {invalidItemActions.length > 0 &&
        <RibbonGroup className="small" >
          {invalidItemActions}
        </RibbonGroup>}
    </>
  ) : undefined;

  return <Ribbon primarySection={parts.primarySection} altSection={altSection} showUser={!select} />;
}
