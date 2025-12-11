import {
  Button,
  Spinner,
  Tooltip,
  mergeClasses,
} from "@fluentui/react-components";
import { AddRegular, CopyAddRegular, CopyRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { NodeItem } from "elza-api";
import { useEffect, useMemo, useRef, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { NodeToolbar } from "./NodeToolbar";
import { DescItemField } from "./desc-items";
import {
  useActiveFund,
  useActiveParent,
  useKeyGen,
  useNodeFormData,
} from "./hooks";
import { useStyles } from "./styles";
import { buildGroups } from "./utils";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { useAppThunkDispatch } from "utils/hooks";
import { ArrDaoVO } from "typings/dao";
import { FormattedMessage, defineMessages } from "react-intl";

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
}

const messages = defineMessages({
  copyFromPrev: {
    id: "desc_item_action_copyFromPrev",
    defaultMessage: "Kopírovat hodnoty PP z předchozí JP",
  },
  copyToggle: {
    id: "desc_item_action_copyToggle",
    defaultMessage: "Nastavení opakovaného kopírování hodnot PP",
  },
});

export function NodeEdit({ fondsVersionId, nodeId, nodeVersionId }: Props) {
  const dispatch = useAppThunkDispatch();
  const activeParent = useActiveParent(); // TODO use different way of getting active parent node
  const styles = useStyles();
  const refs = useRef({});
  const activeFund = useActiveFund();

  const [daos, setDaos] = useState<ArrDaoVO[]>();
  const { getKey, pairKey } = useKeyGen(nodeId);

  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
  const dataTypeRefs = useAppSelector(
    ({ refTables }) => refTables.rulDataTypes.itemsMap,
  );
  const nodeSetting = useAppSelector(({ arrRegion }) =>
    (arrRegion.nodeSettings as any).nodes.find(
      ({ id }) => id === activeParent?.id,
    ),
  ); // TODO add types

  const { formData, nodeData, addEmptyDescItem, deleteDescItem } =
    useNodeFormData(fondsVersionId, nodeId, nodeVersionId);

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

  // build display groups only after groups refs and form data are both loaded
  const viewDescItemGroups = useMemo(() => {
    if (formData && groupRefs) {
      return buildGroups(
        formData,
        groupRefs,
        itemTypeRefs,
        dataTypeRefs,
        nodeId,
        nodeVersionId,
      );
    }
    return [];
  }, [formData, groupRefs, itemTypeRefs, dataTypeRefs, nodeId, nodeVersionId]);

  async function handleDeleteDescItem(item: NodeItem) {
    deleteDescItem(item.id);
  }

  function handleAddDescItemType(descItemType: DescItemTypeRef) {
    addEmptyDescItem(descItemType.id);
  }

  async function handleCopyFromPrev(descItemTypeId: number) {
    await WebApi.copyOlderSiblingAttribute(
      activeFund.versionId,
      nodeId,
      nodeVersionId,
      descItemTypeId,
    );
  }

  async function handleCopyToggle(descItemTypeId: number) {
    const copy = nodeSetting.descItemTypeCopyIds.includes(descItemTypeId);
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

  return (
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
        nodeData={nodeData}
        onAddDescItem={handleAddDescItemType}
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
        {viewDescItemGroups.length === 0 && (
          <div style={{ padding: "50px" }}>
            <Spinner />
          </div>
        )}
        {viewDescItemGroups.map(({ group, descItemTypes }) => {
          return (
            <div style={{ margin: "4px" }} key={group.code}>
              <div
                style={{
                  opacity: 0.5,
                  fontWeight: "bold",
                  fontSize: "0.6rem",
                  padding: "0 4px",
                }}
              >
                {group.name}
              </div>
              <div
                className={styles.gridContainer}
                style={{
                  padding: "8px",
                  background: "var(--shade-0)",
                  borderRadius: "8px",
                  boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
                  display: "grid",
                  // gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
                  // gridTemplateColumns: "repeat(4, 1fr)",
                  // flexWrap: "wrap",
                }}
              >
                {descItemTypes.map(
                  ({ typeRef, typeForm, typeWidth, descItems }) => {
                    return (
                      <div
                        key={typeRef.id}
                        style={{
                          outlineColor: "transparent",
                          outlineOffset: "4px",
                          borderRadius: "1px",
                          transition: "outline-color 300ms ease-out",
                        }}
                        className={mergeClasses(
                          styles.gridItem,
                          styles[`gridItem_${typeWidth}`],
                          styles.descItemTypeTitle,
                        )}
                        onMouseEnter={({ currentTarget }) =>
                          (currentTarget.style.outline = "none")
                        }
                        ref={(el) => {
                          if (refs) refs.current[typeRef.id] = el;
                        }}
                      >
                        <div
                          style={{
                            flexShrink: 1,
                            fontWeight: "bold",
                            marginRight: "4px",
                            display: "flex",
                            alignItems: "flex-end",
                            // opacity: typeWidth ? 1 - (4 - typeWidth) / 6 : 1,
                            // fontSize: `${1 + (typeWidth ? typeWidth * 0.1 : 0.4)}em`,
                          }}
                        >
                          <Tooltip
                            relationship="label"
                            appearance="inverted"
                            content={typeRef.description}
                          >
                            <div>{typeRef.shortcut}</div>
                          </Tooltip>
                          <div className="actions">
                            <Tooltip
                              relationship="label"
                              appearance="inverted"
                              content={
                                <FormattedMessage {...messages.copyFromPrev} />
                              }
                            >
                              <Button
                                className="hidable-button"
                                size="small"
                                appearance="subtle"
                                icon={<CopyAddRegular />}
                                onClick={() => handleCopyFromPrev(typeRef.id)}
                                tabIndex={-1}
                              />
                            </Tooltip>
                            <Tooltip
                              relationship="label"
                              appearance="inverted"
                              content={
                                <FormattedMessage {...messages.copyToggle} />
                              }
                            >
                              <Button
                                className={
                                  nodeSetting?.descItemTypeCopyIds.includes(
                                    typeRef.id,
                                  )
                                    ? undefined
                                    : "hidable-button"
                                }
                                size="small"
                                appearance={
                                  nodeSetting?.descItemTypeCopyIds.includes(
                                    typeRef.id,
                                  )
                                    ? "primary"
                                    : "subtle"
                                }
                                icon={<CopyRegular />}
                                onClick={() => handleCopyToggle(typeRef.id)}
                                tabIndex={-1}
                              />
                            </Tooltip>
                          </div>
                        </div>
                        <div>
                          {descItems
                            .sort(
                              (
                                { position: positionA },
                                { position: positionB },
                              ) => positionA - positionB,
                            )
                            .map((item) => {
                              const itemErrors =
                                nodeData?.nodeConformity.errorList.filter(
                                  ({ descItemObjectId }) =>
                                    descItemObjectId === item.itemObjectId,
                                );

                              const key = getKey(
                                item.itemObjectId ||
                                  `${item.itemTypeId}_${item.itemSpecId}_new`,
                              );

                              function handleItemCreated(item: NodeItem) {
                                pairKey(item.itemObjectId, key);
                              }

                              return (
                                <div key={key}>
                                  <div>
                                    <DescItemField
                                      typeRef={typeRef}
                                      typeForm={typeForm}
                                      item={item}
                                      fondsVersionId={fondsVersionId}
                                      nodeId={nodeId}
                                      nodeVersionId={nodeVersionId}
                                      typeWidth={typeWidth}
                                      errors={itemErrors}
                                      onDelete={handleDeleteDescItem}
                                      onItemCreated={handleItemCreated}
                                    />
                                  </div>
                                  {false && (
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
                                      objId: {item.itemObjectId}, specId:{" "}
                                      {item.itemSpecId}, pos: {item.position},
                                      genKey: {key}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          {typeForm.repeatable &&
                            ((descItems[descItems.length - 1].data?.dataId !=
                              undefined && // last item has data
                              !descItems[descItems.length - 1].undefined) || // last item is not undefined
                              typeRef.useSpecification) && ( // show when item uses specification
                              <Button
                                style={{ borderStyle: "dashed", color: "#666" }}
                                icon={<AddRegular />}
                                onClick={() =>
                                  addEmptyDescItem(
                                    typeRef.id,
                                    descItems[descItems.length - 1].position +
                                      1,
                                  )
                                }
                                tabIndex={-1}
                              >
                                {typeRef.shortcut}
                              </Button>
                            )}
                        </div>
                      </div>
                    );
                  },
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
