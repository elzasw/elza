import { Api } from "api";
import { RulDataTypeVO } from "api/RulDataTypeVO";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import {
  FormItemType,
  ItemDataResult,
  MandatoryType,
  NodeAccordionData,
  NodeData,
  NodeFormData,
  NodeItem,
} from "elza-api";
import { useCallback, useEffect, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { EventType } from "typings/websocket";
import { AnyMessage } from "typings/websocket/Message";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { getOneSettings } from "../ArrUtils";
import { createEmptyDescItem } from "./desc-items/utils";

export function useStrictMode() {
  const strictMode: boolean = useAppSelector(({ userDetail, arrRegion }) => {
    const activeFund = arrRegion.funds[arrRegion.activeIndex];
    const strictModeSetting = getOneSettings(
      userDetail.settings,
      "FUND_STRICT_MODE",
      "FUND",
      activeFund.id,
    );
    const strictModeValue = strictModeSetting
      ? JSON.parse(strictModeSetting?.value)
      : true;
    return strictModeValue == null ? true : strictModeValue;
  });

  return strictMode;
}

export function useActiveFund() {
  const activeFund = useAppSelector(({ arrRegion }) =>
    arrRegion.activeIndex != undefined
      ? arrRegion.funds[arrRegion.activeIndex]
      : undefined,
  );
  return activeFund;
}

export function useActiveParent() {
  const activeFund = useActiveFund();
  const activeParent =
    activeFund.nodes.activeIndex != undefined
      ? activeFund.nodes.nodes[activeFund.nodes.activeIndex]
      : undefined;
  return activeParent;
}

export function useActiveNode() {
  const activeParent = useActiveParent();
  const activeNode = activeParent.childNodes.find(
    ({ id }) => id === activeParent.selectedSubNodeId,
  );
  return activeNode;
}

function useWSNodeChanges(nodeId: number, callback: (version: number) => void) {
  const { addListener, removeListener } = useWebsocket();

  const handleMessage = (message: AnyMessage) => {
    if (
      message.eventType === EventType.NODES_CHANGE &&
      message.entityIds.includes(nodeId)
    ) {
      callback(message.versionId);
    }
  };

  useEffect(() => {
    const listener = addListener(handleMessage);

    return () => {
      removeListener(listener);
    };
  }, []);
}

export function getForcedItemTypes(
  descItems: NodeItem[] = [],
  itemTypes: FormItemType[],
  itemTypeRefs: Record<number, DescItemTypeRef>,
  dataTypeRefs: Record<number, RulDataTypeVO>,
  nodeId: number,
  nodeVersionId: number,
  { skipForcedItems }: { skipForcedItems: boolean },
) {
  const _descItems: NodeItem[] = [];
  const forcedItemTypes = skipForcedItems
    ? []
    : itemTypes.filter(
        ({ type }) =>
          type === MandatoryType.Required || type === MandatoryType.Recommended,
      );

  forcedItemTypes.forEach(({ itemTypeId, specs }) => {
    const itemTypeRef = itemTypeRefs[itemTypeId];
    const dataType = dataTypeRefs[itemTypeRef.dataTypeId];

    const forcedSpecs = specs.filter(
      ({ type }) =>
        type === MandatoryType.Required || type === MandatoryType.Recommended,
    );

    const descItemTypeCount = descItems.filter(
      ({ itemTypeId: _itemTypeId }) => _itemTypeId === itemTypeId,
    ).length;

    if (forcedSpecs.length > 0) {
      forcedSpecs.forEach(({ itemSpecId }) => {
        const descItem = descItems.find(
          ({ itemTypeId: _itemTypeId, itemSpecId: _itemSpecId }) =>
            _itemTypeId === itemTypeId && _itemSpecId === itemSpecId,
        );
        if (!descItem) {
          _descItems.push({
            ...createEmptyDescItem(
              itemTypeId,
              nodeId,
              nodeVersionId,
              descItemTypeCount,
              dataType.code,
            ),
            itemSpecId,
          });
        }
      });
    } else {
      const descItem = descItems.find(
        ({ itemTypeId: _itemTypeId }) => _itemTypeId === itemTypeId,
      );
      if (!descItem) {
        _descItems.push(
          createEmptyDescItem(
            itemTypeId,
            nodeId,
            nodeVersionId,
            descItemTypeCount,
            dataType.code,
          ),
        );
      }
    }
  });
  return _descItems;
}

export interface FormItem {
  item: NodeItem;
  localId: string;
}

function convertToFormItems(
  items: NodeItem[],
  oldItems: FormItem[],
  generateLocalId: (item: NodeItem) => string,
  compare: (oldItem: NodeItem, newItem: NodeItem) => boolean,
): FormItem[] {
  return items.map((item) => {
    const oldItem = oldItems.find(({ item: oldItem }) => {
      return compare(oldItem, item);
      // return item.itemObjectId === oldItem.itemObjectId;
    });

    if (oldItem) {
      return {
        ...oldItem,
        item,
      };
    } else {
      return {
        localId: generateLocalId(item),
        item,
      };
    }
  });
}

export function useNodeFormData(
  fondsVersionId: number,
  nodeId: number,
  nodeVersionId?: number,
  options?: { skipForcedItems: boolean },
) {
  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const dataTypeRefs = useAppSelector(
    ({ refTables }) => refTables.rulDataTypes.itemsMap,
  );

  const { getKey } = useKeyGen(nodeId);

  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [formData, setFormData] = useState<NodeFormData>();
  const [formItems, setFormItems] = useState<FormItem[]>([]);
  const [forcedFormItems, setForcedFormItems] = useState<FormItem[]>([]);
  const [addedFormItems, setAddedFormItems] = useState<FormItem[]>([]);
  const [arrPerm, setArrPerm] = useState<boolean>(false);
  const [itemTypes, setItemTypes] = useState<FormItemType[]>([]);
  const [nodeData, setNodeData] = useState<NodeAccordionData>();
  const [reloadData, setReloadData] = useState<boolean>(true);
  const [markedForClean, setMarkedForClean] = useState<
    { id: number; localId: string }[]
  >([]);
  const [storedData, setStoredData] = useState<NodeData>();

  const applyStoredData = useCallback(
    function (data: NodeData) {
      const _forcedDescItems = options?.skipForcedItems
        ? []
        : getForcedItemTypes(
            [
                ...(data.formData.descItems || []),
                ...addedFormItems.map(({ item }) => item)  // prevent forcing user added descItems
            ],
            data.formData.itemTypes,
            itemTypeRefs,
            dataTypeRefs,
            nodeId,
            nodeVersionId,
            { skipForcedItems: false },
          );

      // setStoredData(undefined);
      setFormData(data.formData);
      setItemTypes(data.formData.itemTypes);
      setFormItems((prevFormItems) => {
        return convertToFormItems(
          data.formData.descItems,
          prevFormItems,
          (item) => {
            const marked = markedForClean.find(
              ({ id }) => id === item.itemObjectId,
            );
            if (marked) {
              return marked.localId;
            }
            return getKey();
          },
          (oldItem, newItem) => oldItem.itemObjectId === newItem.itemObjectId,
        );
      });
      setForcedFormItems((prevForcedFormItems) => {
        return convertToFormItems(
          _forcedDescItems,
          prevForcedFormItems,
          () => getKey(),
          (oldItem, newItem) =>
            oldItem.itemTypeId === newItem.itemTypeId &&
            oldItem.itemSpecId === newItem.itemSpecId,
        );
      });
      setArrPerm(data.formData.arrPerm);
      setNodeData(data.node);
      setAddedFormItems((prevAddedFormItems) => {
        return prevAddedFormItems.filter(({ localId }) => {
          return !markedForClean.find(
            ({ localId: _localId }) => localId === _localId,
          );
        });
      });
    },
    [
      addedFormItems,
      dataTypeRefs,
      itemTypeRefs,
      markedForClean,
      nodeId,
      nodeVersionId,
      options?.skipForcedItems,
      getKey,
    ],
  );

  const fetchAndStoreData = useCallback(
    async function () {
      const { data } = await Api.node.nodeGetNodeData({
        fundVersionId: fondsVersionId,
        nodeId,
        formData: true,
        parents: false,
        children: false,
        siblingsMaxCount: 10,
      });
      setStoredData(data);
    },
    [fondsVersionId, nodeId],
  );

  useEffect(() => {
    if (reloadData) {
      fetchAndStoreData();
      setReloadData(false);
    }
  }, [fondsVersionId, nodeId, reloadData, fetchAndStoreData]);

  useEffect(() => {
    // Serves for synchronizing desc item Create request with NODES_CHANGE event from websocket
    // When the NODES_CHANGE event is received before the response from the Create request, the data is stored and applied
    // after the Create request response is received. If there are multiple NODES_CHANGE events, only the data received last are stored.
    // When storedData change (fetched new data), check whether the form is expecting a response
    // from Create request (isSaving = true), if not, use the stored data.
    // When server finishes saving and returns a response (isSaving > false), use the stored data
    if (!isSaving && storedData) {
      applyStoredData(storedData);
      setStoredData(undefined); // clear stored data
    }
  }, [isSaving, storedData, applyStoredData]);

  useWSNodeChanges(nodeId, () => {
    fetchAndStoreData();
  });

  const ws = useWebsocket();

  function addDescItem(item: NodeItem) {
    setAddedFormItems([...addedFormItems, { localId: getKey(), item }]);
  }

  function addEmptyDescItem(typeId: number, specId?: number, position: number = 1) {
    const typeRef = itemTypeRefs[typeId];
    if (!typeRef) {
      throw `Could not find type ref for id: ${typeId}`;
    }
    const dataType = dataTypeRefs?.[typeRef.dataTypeId];
    if (!dataType) {
      throw `Could not find data type ref for id: ${typeRef.dataTypeId}`;
    }
    if (nodeVersionId == undefined) {
      throw "'NodeVersionId' missing";
    }
    addDescItem(
        {
            ...createEmptyDescItem(
                typeRef.id,
                nodeId,
                nodeVersionId,
                position,
                dataType.code,
            ),
            itemSpecId: specId,
        }
    );
  }

  async function deleteDescItem(item: NodeItem, localId: string) {
    // Item has data, delete from server
    if (item.data?.dataId != undefined || item.undefined) {
      await Api.descItems.descItemDeleteDescItem(fondsVersionId, item);
      return;
    }

    // When item is missing dataId, item is not saved on server and is only deleted locally.
    // First check in user added items
    let index = addedFormItems.findIndex(
      ({ localId: _localId }) => _localId == localId,
    );
    if (index >= 0) {
      const _descItems = [...addedFormItems];
      _descItems.splice(index, 1);
      setAddedFormItems(_descItems);
      return;
    }
    // If the item is not found in the added items, check the forced items
    // (currently not used, it is not possible to reset the value of forced items)
    const _descItems = [...forcedFormItems];
    index = forcedFormItems.findIndex(
      ({ localId: _localId }) => _localId == localId,
    );
    if (index >= 0) {
      const _descItem = _descItems[index].item;
      const emptyDescItem = createEmptyDescItem(
        _descItem.itemTypeId,
        _descItem.nodeId,
        _descItem.nodeVersion,
        _descItem.position,
        _descItem.data.dataType,
      );
      _descItems.splice(index, 1, { localId, item: emptyDescItem });
      setForcedFormItems(_descItems);
    }
    return;
  }

  // function markTypeForClean(localId: string, objectId: number) {
  //   setMarkedForClean([...markedForClean, { localId, id: objectId }]);
  // }

  async function createDescItem(item: NodeItem, localId: string) {
    setIsSaving(true);
    let _data: ItemDataResult;
    try {
      const response = await Api.descItems.descItemCreateDescItem(
        fondsVersionId,
        item,
      );
      _data = response?.data;
    } catch (e) {
      console.error(e);
    } finally {
      setIsSaving(false);
    }
    if (_data) {
      setMarkedForClean([
        ...markedForClean,
        { localId, id: _data.item.itemObjectId },
      ]);

      return _data;
    } else {
      throw "Data missing";
    }
  }

  function updateDescItem(item: NodeItem) {
    return new Promise<void>((resolve, reject) => {
      ws.send(
        `/app/arrangement/descItems/${fondsVersionId}/update/true`,
        JSON.stringify(item),
        () => {
          resolve();
        },
        // TODO Add type
        (error: unknown) => {
          reject(error);
        },
      );
    });
  }

  return {
    formData,
    formItems,
    forcedFormItems,
    addedFormItems,
    itemTypes,
    arrPerm,
    nodeData,
    addDescItem,
    addEmptyDescItem,
    deleteDescItem,
    createDescItem,
    updateDescItem,
    parent: formData?.parent,
  };
}

// Generator stabilnich klicu prvku popisu s moznosti naparovat existujici
// novy prvek popisu na prvek popisu vytvoreny na serveru po ulozeni
let counter = 0;

export function useKeyGen(nodeId: number) {
  useEffect(() => {
    counter = 0;
  }, [nodeId]);

  function getKey() {
    const key = `desc-item-${counter}`;
    counter++;
    return key;
  }

  return { getKey };
}
