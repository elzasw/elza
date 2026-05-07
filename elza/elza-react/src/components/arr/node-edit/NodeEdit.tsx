import { Spinner } from "@fluentui/react-components";
import { WebApi } from "actions";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { useEffect, useMemo, useState } from "react";
import { ArrDaoVO } from "typings/dao";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { FormItemGroup } from "./FormItemGroup";
import { NodeToolbar } from "./NodeToolbar";
import { DescItemTypeFields } from "./DescItemTypeFields";
import { useActiveFund, useActiveParent, useNodeFormData } from "./hooks";
import { NodeFormContext } from "./NodeFormContext";
import { TextFragmentsProvider } from "../text-fragments";
import { useUserSettings } from "contexts/user";
import { buildGroupsForm } from "./utils";
import { useStyles } from "./styles";

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
}

export function NodeEdit({ fondsVersionId, nodeId, nodeVersionId }: Props) {
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

  const nodeFormData = useNodeFormData(fondsVersionId, nodeId, nodeVersionId);
  const {
    formData,
    formItems,
    forcedFormItems,
    addedFormItems,
    itemTypes,
    nodeData,
    addEmptyDescItem,
    deleteDescItem,
    createDescItem,
    updateDescItem,
    parent,
  } = nodeFormData;

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
      <div style={{ padding: compact ? "4px 8px" : "8px", columns: `350px ${settings.groupColumns || 1}` }}>
        {viewDescItemGroupsLocal.length === 0 && (
          <div className={styles.spinnerPadding}>
            <Spinner />
          </div>
        )}
        {viewDescItemGroupsLocal.map(({ group, descItemTypes }) => {
          return (
            <FormItemGroup group={group}>
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
                />
              ))}
            </FormItemGroup>
          );
        })}
      </div>
    </div>
    </NodeFormContext.Provider>
    </TextFragmentsProvider>
  );
}
