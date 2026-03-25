import {
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  MenuPopover,
  MenuTrigger,
  Overflow,
  Toolbar,
  ToolbarButton,
  ToolbarDivider,
  Tooltip,
} from "@fluentui/react-components";
import {
  AddRegular,
  ArrowSyncRegular,
  ColumnRegular,
  CommentRegular,
  CopyRegular,
  DeleteRegular,
  HistoryRegular,
  LayoutColumnTwoRegular,
  LayoutColumnThreeRegular,
  LayoutColumnFourRegular,
  LinkMultipleRegular,
  PaddingDownRegular,
  PaddingTopRegular,
  SettingsCogMultipleRegular,
  SubtractRegular,
  TextQuoteRegular,
} from "@fluentui/react-icons";
import { WebApi } from "actions";
import * as issuesActions from "actions/arr/issues";
import { deleteNode } from "actions/arr/node";
import { toggleCopyAllDescItemType } from "actions/arr/nodeSetting";
import { fundSubNodeDaoChangeScenario } from "actions/arr/subNodeDaos";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { routerNavigate } from "actions/router";
import IssueForm from "components/form/IssueForm";
import i18n from "components/i18n";
import { showConfirmDialog } from "components/shared/dialog";
import ConfirmForm from "components/shared/form/ConfirmForm";
import {
  FormItemType,
  NodeAccordionData,
  NodeBase,
  NodeFormData,
} from "elza-api";
import { useState } from "react";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { IssueVO } from "types";
import { ArrDaoVO } from "typings/dao";
import { DescItemTypeRef } from "typings/store";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { urlFundNode } from "../../../constants";
import ArrHistoryForm from "../ArrHistoryForm";
import ArrRequestForm from "../ArrRequestForm";
import { isFundRootId } from "../ArrUtils";
import SyncNodes from "../SyncNodes";
import { NodeSettingsModal } from "../node-settings-form";
import { QuoteModal, messages as quoteMessages } from "../quote";
import { TextFragmentsWindow } from "../text-fragments";
import { AddDescItemTypeForm } from "./AddDescItemType";
import {
  OverflowMenu,
  ToolbarButtonGroupDef,
  ToolbarOverflowButton,
  // ToolbarOverflowDivider,
} from "./ToolbarOverflow";
import { FormItem, useActiveFund, useActiveParent } from "./hooks";
import { useTemplates } from "./templates/templates";
import { useUserSettings } from "contexts/user";

export const messages = defineMessages({
  addDescItem: {
    id: "node_action_addDescItem",
    defaultMessage: "Prvek popisu",
  },
  toggleCopyFromPrevious: {
    id: "node_action_toggleCopyFromPrevious",
    defaultMessage: "Nastavení opakovaného kopírování všech hodnot PP",
  },
  visiblePolicy: {
    id: "node_action_visiblePolicy",
    defaultMessage: "Pravidla kontroly",
  },
  showHistory: {
    id: "node_action_showHistory",
    defaultMessage: "Historie",
  },
  deleteNode: {
    id: "node_action_deleteNode",
    defaultMessage: "Smazat jednotku popisu",
  },
  showSymbols: {
    id: "node_action_showSymbols",
    defaultMessage: "Symboly",
  },
  showQuotes: {
    id: "node_action_showQuotes",
    defaultMessage: "Citace",
  },
  addComment: {
    id: "node_action_addComment",
    defaultMessage: "Přidat připomínku",
  },
  createTemplate: {
    id: "node_action_createTemplate",
    defaultMessage: "Vytvořit šablonu",
  },
  applyTemplate: {
    id: "node_action_applyTemplate",
    defaultMessage: "Použít šablonu",
  },
  copyUUID: {
    id: "node_action_copyUUID",
    defaultMessage: "Kopirovat UUID jednotky popisu",
  },
  syncNode: {
    id: "node_action_syncNode",
    defaultMessage: "Synchronizovat JP ze zdrojovych AS",
  },
  digitizationRequest: {
    id: "node_action_digitizationRequest",
    defaultMessage: "Požadavek na digitalizaci",
  },
  digitizationSync: {
    id: "node_action_digitizationSync",
    defaultMessage: "Synchronizovat DAO",
  },
  toggleCompact: {
    id: "node_action_toggleCompact",
    defaultMessage: "Kompaktní zobrazení",
  },
  addColumn: {
    id: "node_action_addColumn",
    defaultMessage: "Přidat sloupec skupin",
  },
  removeColumn: {
    id: "node_action_removeColumn",
    defaultMessage: "Odebrat sloupec skupin",
  },
});

export interface Props {
  formData?: NodeFormData;
  itemTypes?: FormItemType[];
  formItems?: FormItem[];
  parent?: NodeBase;
  nodeData?: NodeAccordionData;
  onAddDescItem: (itemTypeId: number, itemSpecId?: number) => void;
  daos?: ArrDaoVO[];
}

export const NodeToolbar = ({
  formItems,
  itemTypes,
  parent,
  nodeData,
  onAddDescItem,
  daos = [],
}: Props) => {
  const descItems = formItems.map(({ item }) => item);

  const [showSpecialCharactersWindow, setShowSpecialCharactersWindow] =
    useState<boolean>(false);
  const dispatch = useAppThunkDispatch();
  const activeFund = useActiveFund();
  const activeParent = useActiveParent(); // TODO use different way of getting active parent node
    const { createTemplate, applyTemplate } = useTemplates({
        descItems,
        nodeId: activeParent.selectedSubNodeId,
        nodeVersion: activeParent.version,
        fondsVersionId: activeFund.versionId,
        onAddDescItem,
    });
  const { settings, update: updateSettings } = useUserSettings();
  const { formatMessage } = useIntl();

  const issueProtocol = useAppSelector(({ app }) => app.issueProtocol as any); // TODO add types
  const issueTypes = useAppSelector(({ refTables }) => refTables.issueTypes);
  const nodeSetting = useAppSelector(({ arrRegion }) =>
    (arrRegion.nodeSettings as any).nodes.find(
      ({ id }) => id === activeParent?.id,
    ),
  ); // TODO add types

  const notRoot = !isFundRootId(activeParent.id);

  function handleAddDescItem() {
    dispatch(
      modalDialogShow(this, undefined, ({ onClose }) => {
        function handleSubmit(item: DescItemTypeRef) {
          onAddDescItem(item.id);
          onClose();
        }
        return (
          <AddDescItemTypeForm
            itemTypes={itemTypes}
            descItems={descItems}
            onClose={onClose}
            onSubmit={handleSubmit}
          />
        );
      }),
    );
  }

  function handleToggleCopyFromPrevious() {
    dispatch(toggleCopyAllDescItemType(activeParent.id));
  }

  function handleShowHistory() {
    function handleDeleteChanges(nodeId, fromChangeId, toChangeId) {
      return WebApi.revertChanges(
        activeFund.versionId,
        nodeId,
        fromChangeId,
        toChangeId,
      );
    }

    dispatch(
      modalDialogShow(
        this,
        i18n("arr.history.title"),
        <ArrHistoryForm
          locked={false}
          versionId={activeFund.versionId}
          node={{
            ...nodeData,
          }}
          onDeleteChanges={handleDeleteChanges}
        />,
        "dialog-lg",
      ),
    );
  }

  async function handleDeleteNode() {
    const response = await dispatch(
      showConfirmDialog(i18n("arr.fund.deleteNode.confirm")),
    );

    if (response) {
      dispatch(
        deleteNode(nodeData, activeParent, activeFund.versionId, async () => {
          let nodeId = activeParent.id;
          if (activeParent.childNodes.length > 1) {
            const index = activeParent.childNodes.findIndex(
              ({ id }: any) => id === nodeData.id,
            ); // TODO add types
            if (index > 0) {
              nodeId = (activeParent.childNodes[index - 1] as any).id; // TODO add types
            } else {
              nodeId = (activeParent.childNodes[index + 1] as any).id; // TODO add types
            }
          }
          console.log("#nt", activeParent.childNodes, nodeId);
          // const data = await WebApi.selectNode(nodeId);
          // console.log("#nt - after select", data);
          dispatch(
            routerNavigate(urlFundNode(activeFund.id, undefined, nodeId)),
          );
          // dispatch(processNodeNavigation(data, activeFund.versionId));
        }),
      );
    }
  }

  function handleShowSpecialCharacters() {
    setShowSpecialCharactersWindow(!showSpecialCharactersWindow);
  }

  function handleQuote() {
    dispatch(
      modalDialogShow(
        this,
        <FormattedMessage {...quoteMessages.quoteTitle} />,
        <QuoteModal versionId={activeFund.versionId} nodeId={nodeData.id} />, // TODO add types
        null,
      ),
    );
  }

  function handleCreateIssueNode() {
    dispatch(
      modalDialogShow(
        this,
        i18n("arr.issues.add.node.title"),
        <IssueForm
          onSubmit={(data: IssueVO) =>
            WebApi.addIssue({
              ...data,
              nodeId: nodeData.id,
            })
          }
          initialValues={{
            issueListId: issueProtocol.id,
            issueTypeId: issueTypes?.data?.[0].id,
          }}
          onSubmitSuccess={(data) => {
            dispatch(issuesActions.list.invalidate(data.issueListId));
            dispatch(issuesActions.detail.invalidate(data.id));
            dispatch(modalDialogHide());
          }}
        />,
      ),
    );
  }

  function handleCreateTemplate() {
    createTemplate();
  }

  function handleApplyTemplate() {
    applyTemplate();
  }

  function handleDigitizationRequest() {
    const nodeId = nodeData.id;
    const versionId = activeFund.versionId;

    const form = (
      <ArrRequestForm
        fundVersionId={versionId}
        type="DIGITIZATION"
        onSubmitForm={(send: any, data: any) => {
          //TODO add types
          return WebApi.arrDigitizationRequestAddNodes(
            versionId,
            data.requestId,
            send,
            data.description,
            [nodeId],
            parseInt(data.digitizationFrontdesk),
          );
        }}
        onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
      />
    );
    dispatch(
      modalDialogShow(
        this,
        i18n("arr.request.digitizationRequest.form.title"),
        form,
      ),
    );
  }

  function handleDigitizationSync() {
    const nodeId = nodeData.id;
    const versionId = activeFund.versionId;

    const confirmForm = (
      <ConfirmForm
        confirmMessage={i18n("arr.daos.node.sync.confirm-message")}
        submittingMessage={i18n("arr.daos.node.sync.submitting-message")}
        submitTitle={i18n("global.action.run")}
        onSubmit={async () => {
          const result = await WebApi.syncDaoLink(versionId, nodeId);
          dispatch(modalDialogHide());
          return result;
        }}
      />
    );
    dispatch(
      modalDialogShow(this, i18n("arr.daos.node.sync.title"), confirmForm),
    );
  }

  function handleRefSync() {
    const nodeId = nodeData.id;
    let nodeVersion = nodeData.version;

    if (!nodeVersion) {
      console.error("Nedohledána verze pro JP", nodeId);
      // const subNode = objectById(node.childNodes, nodeId);
      // if (subNode == null) {
      //     console.error("Nedohledána verze pro JP", nodeId);
      // } else {
      //     nodeVersion = subNode.version;
      // }
    }

    dispatch(
      modalDialogShow(
        this,
        i18n("arr.syncNodes.title"),
        <SyncNodes
          nodeId={nodeId}
          nodeVersion={nodeVersion}
          fundId={activeFund.id}
        />,
      ),
    );
  }

  async function handleCopyUuid() {
    await navigator.clipboard.writeText(parent.uuid);
  }

  function handleToggleCompact() {
    updateSettings({ compact: !settings.compact });
  }

  function handleAddColumn() {
    const current = settings.groupColumns || 1;
    const next = current < 4 ? current + 1 : 0;
    updateSettings({ groupColumns: next });
  }

  // function handleRemoveColumn() {
  //   const current = settings.groupColumns || 1;
  //   if (current > 1) {
  //     updateSettings({ groupColumns: current - 1 });
  //   }
  // }

  function handleVisiblePolicy() {
    dispatch(
      modalDialogShow(
        this,
        i18n("visiblePolicy.form.title"),
        <NodeSettingsModal
          nodeId={nodeData?.id}
          fundVersionId={activeFund.versionId}
          onClose={() => {}}
          onSubmitSuccess={() => {}}
          onSubmit={() => {}}
        />,
      ),
    );
  }

  const buttonDefs: ToolbarButtonGroupDef[] = [
    {
      groupId: "1",
      items: [
        {
          label: formatMessage(messages.addDescItem),
          showLabel: true,
          icon: <AddRegular />,
          appearance: "primary",
          id: "add-desc-Item",
          action: handleAddDescItem,
        },
      ],
    },
    {
      groupId: "2",
      items: [
        {
          label: formatMessage(messages.toggleCopyFromPrevious),
          showLabel: false,
          icon: <CopyRegular />,
          appearance: nodeSetting?.copyAll ? "primary" : "subtle",
          id: "copy",
          action: handleToggleCopyFromPrevious,
        },
        {
          label: formatMessage(messages.visiblePolicy),
          showLabel: false,
          icon: <SettingsCogMultipleRegular />,
          appearance: "subtle",
          id: "visible-policy",
          action: handleVisiblePolicy,
        },
        {
          label: formatMessage(messages.showHistory),
          showLabel: false,
          icon: <HistoryRegular />,
          appearance: "subtle",
          id: "history",
          action: handleShowHistory,
        },
        {
          label: formatMessage(messages.deleteNode),
          showLabel: false,
          icon: <DeleteRegular />,
          appearance: "subtle",
          id: "delete-node",
          action: handleDeleteNode,
          isVisible: notRoot,
        },
      ],
    },
    {
      groupId: "3",
      items: [
        {
          label: formatMessage(messages.showSymbols),
          showLabel: false,
          icon: <span>Ω</span>,
          appearance: "subtle",
          id: "symbols",
          action: handleShowSpecialCharacters,
          isVisible: true,
        },
      ],
    },
    {
      groupId: "4",
      items: [
        {
          label: formatMessage(messages.showQuotes),
          showLabel: true,
          icon: <TextQuoteRegular />,
          appearance: "subtle",
          id: "quotes",
          action: handleQuote,
        },
      ],
    },
    {
      groupId: "5",
      items: [
        {
          label: formatMessage(messages.addComment),
          showLabel: false,
          icon: <CommentRegular />,
          appearance: "subtle",
          id: "comment",
          action: handleCreateIssueNode,
        },
      ],
    },
    {
      groupId: "6",
      items: [
        {
          label: formatMessage(messages.createTemplate),
          showLabel: true,
          icon: <AddRegular />,
          appearance: "subtle",
          id: "create-template",
          action: handleCreateTemplate,
          isVisible: true,
          overflowOnly: true,
        },
        {
          label: formatMessage(messages.applyTemplate),
          showLabel: true,
          appearance: "subtle",
          id: "apply-template",
          action: handleApplyTemplate,
          isVisible: true,
          overflowOnly: true,
        },
        {
          label: formatMessage(messages.copyUUID),
          showLabel: true,
          icon: <CopyRegular />,
          appearance: "subtle",
          id: "copy-uuid",
          action: handleCopyUuid,
          overflowOnly: true,
        },
      ],
    },
    // {
    //   groupId: "7",
    //   items: [
    //     {
    //       label: formatMessage(messages.digitizationRequest),
    //       showLabel: true,
    //       icon: <CameraAddRegular />,
    //       appearance: "subtle",
    //       id: "digitization-request",
    //       action: handleDigitizationRequest,
    //       isVisible: false,
    //     },
    //     {
    //       label: formatMessage(messages.digitizationSync),
    //       showLabel: true,
    //       icon: <CameraRegular />,
    //       appearance: "subtle",
    //       id: "digitization-sync",
    //       action: handleDigitizationSync,
    //       isVisible: daos.length > 0,
    //     },
    //   ],
    // },
    {
      groupId: "8",
      items: [
        {
          label: formatMessage(messages.syncNode),
          showLabel: true,
          icon: <ArrowSyncRegular />,
          appearance: "subtle",
          id: "sync-node",
          action: handleRefSync,
          overflowOnly: true,
        },
      ],
    },
  ];

  const handleSelectScenario = (daoId: number, scenario: any) =>
    dispatch(
      fundSubNodeDaoChangeScenario(
        daoId,
        scenario,
        activeFund.versionId,
        nodeData.id,
      ),
    );

  function getDaoWithScenario() {
    if (daos.length <= 0) {
      return null;
    }

    const dao = daos[0]; // je predpoklad, ze pri pouziti scenaru je napojeny soubor vzdy jen 1
    return dao.daoLink?.scenario ? dao : null;
  }

  // remove empty groups
  const _buttonDefs = buttonDefs.filter(
    ({ items }) => items.filter(({ isVisible = true }) => isVisible).length > 0,
  );
  // remove hidden buttons
  const filteredButtonDefs = _buttonDefs.map((buttonDef) => ({
    ...buttonDef,
    items: buttonDef.items.filter(({ isVisible = true }) => isVisible),
  }));

  const daoWithScenario = getDaoWithScenario();

  return (
    <>
      <div
        style={{
          position: "sticky",
          top: 0,
          zIndex: 100,
          padding: "8px",
          background: "var(--shade-1)",
          display: "flex",
          alignItems: "center",
        }}
      >
        <div style={{ flex: 1, minWidth: 0, overflow: "hidden", paddingRight: "8px" }}>
        <Overflow padding={20}>
          <Toolbar aria-label="Overflow" size="small">
            {/*<Button>test</Button>*/}
            {daoWithScenario?.scenarios && (
              <Menu>
                <MenuTrigger disableButtonEnhancement={true}>
                  <MenuButton
                    size="small"
                    title={i18n("subNodeDao.dao.action.changeScenario")}
                    style={{
                      whiteSpace: "nowrap",
                      marginRight: "4px",
                      flexShrink: 0,
                    }}
                    icon={<LinkMultipleRegular />}
                  >
                    {/*{i18n("subNodeDao.dao.action.changeScenario")}*/}
                  </MenuButton>
                </MenuTrigger>
                <MenuPopover>
                  <MenuList>
                    {daoWithScenario.scenarios.map((scenario) => {
                      return (
                        <MenuItem
                          onClick={() =>
                            handleSelectScenario(daos?.[0].id, scenario)
                          }
                        >
                          {scenario}
                        </MenuItem>
                      );
                    })}
                  </MenuList>
                </MenuPopover>
              </Menu>
            )}
            {filteredButtonDefs.map(({ groupId, items }, index) => {
              // const isLast = index === filteredButtonDefs.length - 1;
              return (
                <>
                  {items.filter(({ overflowOnly }) => !overflowOnly).map(
                    ({ label, icon, appearance, id, action, showLabel }, itemIndex) => {
                      return (
                        <ToolbarOverflowButton
                          overflowId={id}
                          overflowGroupId={groupId}
                          appearance={appearance}
                          onClick={action}
                          icon={icon}
                          tooltip={!showLabel && label}
                          showDivider={itemIndex === 0 && index > 0}
                        >
                          {showLabel ? label : undefined}
                        </ToolbarOverflowButton>
                      );
                    },
                  )}
                  {/*{!isLast && <ToolbarOverflowDivider groupId={groupId} />}*/}
                </>
              );
            })}

            <OverflowMenu items={filteredButtonDefs} />
          </Toolbar>
        </Overflow>
        </div>
        {settings.showExperimentalFeatures && <Toolbar aria-label="View settings" size="small" style={{ flexShrink: 0 }}>
          <Tooltip appearance="inverted" relationship="label" content={formatMessage(messages.toggleCompact)}>
            <ToolbarButton
              appearance={"subtle"}
              icon={settings.compact ? <PaddingTopRegular /> : <PaddingDownRegular />}
              onClick={handleToggleCompact}
            />
          </Tooltip>
          <ToolbarDivider />
          <Tooltip appearance="inverted" relationship="label" content={`${formatMessage(messages.addColumn)} (${settings.groupColumns || 1})`}>
            <ToolbarButton
              appearance="subtle"
              icon={<span style={{ position: "relative", display: "inline-flex", alignItems: "center" }}>
                {(() => {
                  const cols = settings.groupColumns || 1;
                  if (cols >= 4) return <LayoutColumnFourRegular />;
                  if (cols === 3) return <LayoutColumnThreeRegular />;
                  if (cols === 2) return <LayoutColumnTwoRegular />;
                  return <ColumnRegular />;
                })()}
              </span>}
              onClick={handleAddColumn}
            />
          </Tooltip>
        </Toolbar>}
      </div>
      {showSpecialCharactersWindow && (
        <TextFragmentsWindow
          onClose={() => {
            setShowSpecialCharactersWindow(false);
          }}
        />
      )}
    </>
  );
};
