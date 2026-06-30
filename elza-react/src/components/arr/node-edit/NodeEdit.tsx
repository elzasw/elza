import { Spinner } from "@fluentui/react-components";
import { WebApi } from "actions";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { useEffect, useMemo, useState } from "react";
import { NodeFormData, NodeStatus } from "elza-api";
import { ArrDaoVO } from "typings/dao";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { FormItemGroup } from "./FormItemGroup";
import { GroupColumns } from "./GroupColumns";
import { NodeToolbar } from "./NodeToolbar";
import { DescItemTypeFields } from "./DescItemTypeFields";
import { useActiveFund, useActiveParent, useNodeFormData } from "./hooks";
import { NodeFormContext } from "./NodeFormContext";
import { TextFragmentsProvider } from "../text-fragments";
import { useUserSettings } from "contexts/user";
import { buildGroupsForm } from "./utils";
import { useStyles } from "./styles";
import DaoLinkDetail from "components/aip/DaoLinkDetail";

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
  /** When provided, NodeEdit waits for `seedFormData`/`seedNodeStatus` from parent instead of fetching. */
  seedFromParent?: boolean;
  seedFormData?: NodeFormData;
  seedNodeStatus?: NodeStatus;
  /** Called when the form requests a refresh (e.g. websocket NODES_CHANGE). Parent re-fetches and re-seeds. */
  onRefresh?: () => void;
}

export function NodeEdit({ fondsVersionId, nodeId, nodeVersionId, seedFromParent, seedFormData, seedNodeStatus, onRefresh }: Props) {
  const dispatch = useAppThunkDispatch();
  const activeParent = useActiveParent(); // TODO use different way of getting active parent node
  const activeFund = useActiveFund();
  const { settings } = useUserSettings();
  const compact = settings.compact;
  const styles = useStyles();

  const [daos, setDaos] = useState<ArrDaoVO[]>();

  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
  const nodeSetting = useAppSelector(({ arrRegion }) =>
    arrRegion.nodeSettings.nodes.find(({ id }) => id === activeParent?.id),
  );

  const nodeFormData = useNodeFormData(fondsVersionId, nodeId, nodeVersionId, {
    seedFromParent,
    seedFormData,
    seedNodeStatus,
    onRefresh,
  });
  const {
    formData,
    formItems,
    forcedFormItems,
    addedFormItems,
    itemTypes,
    nodeData,
    addEmptyDescItem: addEmptyDescItemBase,
    deleteDescItem,
    createDescItem,
    updateDescItem,
    parent,
  } = nodeFormData;

  // localId of a freshly added field that should receive focus once it has mounted.
  // Tracked here (rather than in a single DescItemTypeFields) so both add paths work:
  // the per-type "+" button and the "add item type" modal, which add to different
  // DescItemTypeFields instances.
  const [autoFocusLocalId, setAutoFocusLocalId] = useState<string>();

  function addEmptyDescItem(typeId: number, specId?: number, position?: number) {
    const localId = addEmptyDescItemBase(typeId, specId, position);
    // When several types are added at once (the "add item type" modal allows a
    // multi-select), keep the first one as the focus target so focus lands on the
    // topmost new field rather than the last.
    setAutoFocusLocalId((current) => current ?? localId);
    return localId;
  }

  useEffect(() => {
    if (nodeData?.id) {
      (async function () {
        const result = await WebApi.getFundNodeDaos(
          activeFund.versionId,
          nodeData?.id,
        );
        setDaos(result);
      })();
    }
  }, [nodeData?.id, activeFund.versionId]);

  const viewDescItemGroupsLocal = useMemo(() => {
    if (formItems && groupRefs) {
      return buildGroupsForm(
        [...formItems, ...forcedFormItems, ...addedFormItems],
        itemTypes,
        groupRefs,
        itemTypeRefs,
      );
    }
    return [];
  }, [
    groupRefs,
    itemTypeRefs,
    addedFormItems,
    formItems,
    forcedFormItems,
    itemTypes,
  ]);

  async function handleCopyFromPrev(descItemTypeId: number) {
    await WebApi.copyOlderSiblingAttribute(
      activeFund.versionId,
      nodeId,
      nodeVersionId,
      descItemTypeId,
    );
  }

  async function handleCopyToggle(descItemTypeId: number) {
    const copy = nodeSetting?.descItemTypeCopyIds?.includes(descItemTypeId);
    if (!copy) {
      dispatch(copyDescItemType(activeParent.id, descItemTypeId));
    } else {
      dispatch(nocopyDescItemType(activeParent.id, descItemTypeId));
    }
  }

  // function scrollDescItemIntoView(typeId: number){
  //   const element = refs?.current[typeId];
  //   if (element) {
  //     element.scrollIntoView({ behavior: "smooth", block: "center" });
  //     element.style.outline = "3px solid var(--accent-color)";
  //     setTimeout(() => (element.style.outline = "3px solid transparent"), 500);
  //   }
  // }
  //

  const isFirstNode =
    activeParent.childNodes.findIndex((node: any) => node.id === nodeId) === 0; // TODO add types

  return (
    <TextFragmentsProvider>
    <NodeFormContext.Provider value={nodeFormData}>
    <div className={styles.nodeEditForm}>
      <NodeToolbar
        formData={formData}
        formItems={[...formItems, ...forcedFormItems, ...addedFormItems]}
        itemTypes={itemTypes}
        parent={parent}
        nodeData={nodeData}
        onAddDescItem={addEmptyDescItem}
        daos={daos}
      />
      <DaoLinkDetail nodeId={nodeId} />
      {/* <div
        style={{
          position: "fixed",
          background: "white",
          padding: "16px",
          left: "50px",
          boxShadow: "4px 4px 8px 0 #0003",
          borderRadius: "8px",
        }}
      >
        {viewDescItemGroups.map(({ descItemTypes }) => {
          return descItemTypes.map(({ typeRef }) => {
            return (
              <div
                style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                onClick={() => scrollDescItemIntoView(typeRef.id)}
                title={typeRef.name}
              >
                {typeRef.shortcut}
              </div>
            );
          });
        })}
      </div> */}
      <div style={{ padding: compact ? "4px 8px" : "8px" }}>
        {viewDescItemGroupsLocal.length === 0 && (
          <div className={styles.spinnerPadding}>
            <Spinner />
          </div>
        )}
        <GroupColumns groups={viewDescItemGroupsLocal} columnCount={settings.groupColumns || 1}>
          {({ group, descItemTypes }) => (
            <FormItemGroup key={group.code} group={group}>
              {descItemTypes.map(({ typeRef, typeForm, typeWidth, descItems }) => (
                <DescItemTypeFields
                  key={typeRef.id}
                  typeRef={typeRef}
                  typeForm={typeForm}
                  typeWidth={typeWidth}
                  descItems={descItems}
                  fondsVersionId={fondsVersionId}
                  nodeId={nodeId}
                  nodeVersionId={nodeVersionId}
                  nodeSetting={nodeSetting}
                  isFirstNode={isFirstNode}
                  handleCopyFromPrev={handleCopyFromPrev}
                  handleCopyToggle={handleCopyToggle}
                  addEmptyDescItem={addEmptyDescItem}
                  deleteDescItem={deleteDescItem}
                  createDescItem={createDescItem}
                  updateDescItem={updateDescItem}
                  autoFocusLocalId={autoFocusLocalId}
                  onAutoFocusTaken={() => setAutoFocusLocalId(undefined)}
                />
              ))}
            </FormItemGroup>
          )}
        </GroupColumns>
      </div>
    </div>
    </NodeFormContext.Provider>
    </TextFragmentsProvider>
  );
}
