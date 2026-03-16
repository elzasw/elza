import {
    Button,
    Spinner
} from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { useEffect, useMemo, useState } from "react";
import { ArrDaoVO } from "typings/dao";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { DraggableList } from "./DraggableList";
import { FormItemGroup } from "./FormItemGroup";
import { FormItemTypeComp } from "./FormItemType";
import { NodeToolbar } from "./NodeToolbar";
import { DescItemField } from "./desc-items";
import { useActiveFund, useActiveParent, useNodeFormData } from "./hooks";
import { NodeFormContext } from "./NodeFormContext";
import { buildGroupsForm } from "./utils";

const SHOW_DEBUG_DATA = false;

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
}

export function NodeEdit({ fondsVersionId, nodeId, nodeVersionId }: Props) {
  const dispatch = useAppThunkDispatch();
  const activeParent = useActiveParent(); // TODO use different way of getting active parent node
  const activeFund = useActiveFund();

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
    <NodeFormContext.Provider value={nodeFormData}>
    <div
      style={{
        background: "var(--shade-1)",
        containerName: "form-container",
        containerType: "inline-size",
        position: "relative",
      }}
    >
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
      <div style={{ padding: "8px" }}>
        {viewDescItemGroupsLocal.length === 0 && (
          <div style={{ padding: "50px" }}>
            <Spinner />
          </div>
        )}
        {viewDescItemGroupsLocal.map(({ group, descItemTypes }) => {
          return (
            <FormItemGroup group={group}>
              {descItemTypes.map(
                ({ typeRef, typeForm, typeWidth, descItems }) => {
                    function handleChangeOrder(index: number, newIndex: number) {
                        const item = descItems[index].item;
                        let newPosition = descItems[newIndex]?.item.position;

                        // if new position is empty add to end
                        // (newIndex is higher than number of descItems)
                        if (!newPosition) {
                            newPosition = descItems[descItems.length - 1].item.position + 1;
                        }

                        // subtract self from final position number, when moving down
                        if (newPosition > item.position) {
                            newPosition = newPosition - 1;
                        }
                        updateDescItem({ ...item, position: newPosition });
                    }

                  return (
                    <FormItemTypeComp
                      typeForm={typeForm}
                      typeRef={typeRef}
                      typeWidth={typeWidth}
                      nodeSettings={nodeSetting}
                      handleCopyFromPrev={handleCopyFromPrev}
                      handleCopyToggle={handleCopyToggle}
                      canCopyFromPrev={isFirstNode}
                    >
                          <DraggableList
                              canPlaceBeforeItem={(index) => descItems[index].item.position > 0}
                              isItemDraggable={(index) => descItems[index].item.position > 0 && (descItems[index].item.data?.dataId != undefined || descItems[index].item.undefined)}
                              onChangeOrder={handleChangeOrder}
                          >
                              {descItems
                        .sort(
                          (
                            { item: { position: positionA } },
                            { item: { position: positionB } },
                          ) => positionA - positionB,
                        )
                        .map(({ item, localId }) => {
                          return (
                            <div key={localId} style={{container: "desc-item-container"}}>
                              <div>
                                <DescItemField
                                typeRef={typeRef}
                                typeForm={typeForm}
                                item={item}
                                fondsVersionId={fondsVersionId}
                                nodeId={nodeId}
                                nodeVersionId={nodeVersionId}
                                typeWidth={typeWidth}
                                onDelete={(item) =>
                                  deleteDescItem(item, localId)
                                }
                                onCreate={(item) =>
                                  createDescItem(item, localId)
                                }
                                onUpdate={(item) =>
                                  updateDescItem(item, localId)
                                }
                                />
                              </div>
                              {SHOW_DEBUG_DATA && (
                                <div
                                  style={{
                                    background: "var(--shade-3)",
                                    display: "inline-block",
                                    padding: "4px",
                                    lineHeight: "1em",
                                    borderRadius: "4px",
                                    border: "var(--primary-border)",
                                  }}
                                >
                                    typeId: {item.itemTypeId}, objId: {item.itemObjectId}, specId:{" "}
                                  {item.itemSpecId}, pos: {item.position},
                                  genKey: {localId}
                                </div>
                              )}
                            </div>
                          );
                        })}
                        </DraggableList>
                          {typeForm.repeatable &&
                            ((descItems[descItems.length - 1]?.item.data
                              ?.dataId != undefined && // last item has data
                              !descItems[descItems.length - 1]?.item
                                .undefined) || // last item is not undefined
                              typeRef.useSpecification) && ( // show when item uses specification
                              <Button
                                style={{ borderStyle: "dashed", color: "#666", margin: "2px 0" }}
                                icon={<AddRegular />}
                                onClick={() =>{
                                    const lastItem = descItems[descItems.length - 1].item;
                                    const nextPosition = lastItem.position > 0 ? lastItem.position + 1 : 1;
                                    addEmptyDescItem(
                                        typeRef.id,
                                        undefined,
                                        nextPosition,
                                    )
                                }
                                }
                                tabIndex={-1}
                              >
                                {typeRef.shortcut}
                              </Button>
                            )}
                    </FormItemTypeComp>
                  );
                },
              )}
            </FormItemGroup>
          );
        })}
      </div>
    </div>
    </NodeFormContext.Provider>
  );
}
