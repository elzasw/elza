import { useAppSelector } from "utils/hooks/useAppSelector";
import { getOneSettings } from "../ArrUtils";
import { useCallback, useEffect, useState } from "react";
import { Api } from "api";
import { NodeAccordionData, NodeFormData, NodeItem } from "elza-api";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { AnyMessage } from "typings/websocket/Message";
import { EventType } from "typings/websocket";
import { createEmptyDescItem } from "./desc-items/utils";
import { DescItemTypeRef } from "typings/store";

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

export function useNodeFormData(
  fondsVersionId: number,
  nodeId: number,
  nodeVersionId?: number,
) {
  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  // const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
  const dataTypeRefs = useAppSelector(
    ({ refTables }) => refTables.rulDataTypes.itemsMap,
  );

  const [formData, setFormData] = useState<NodeFormData>();
  const [nodeData, setNodeData] = useState<NodeAccordionData>();
  const [reloadData, setReloadData] = useState<boolean>(true);

  const loadData = useCallback(async () => {
    const { data } = await Api.node.nodeGetNodeData({
      fundVersionId: fondsVersionId,
      nodeId,
      formData: true,
      parents: false,
      children: false,
      siblingsMaxCount: 10,
    });

    setFormData(data.formData);
    setNodeData(data.node);
  }, [fondsVersionId, nodeId]);

  useEffect(() => {
    if (reloadData) {
      loadData();
    }
    setReloadData(false);
  }, [fondsVersionId, nodeId, loadData, reloadData]);

  useWSNodeChanges(nodeId, () => {
    loadData();
  });

  function addDescItem(item: NodeItem) {
    if (nodeVersionId == undefined) {
      throw "'NodeVersionId' missing";
    }

    const _formData = {
      ...formData,
      descItems: [...formData.descItems, item],
    };
    setFormData(_formData);
  }

  function addEmptyDescItem(typeId: number, position: number = 1) {
    const typeRef = itemTypeRefs[typeId];
    if (!typeRef) {
      throw `Could not find type ref for id: ${typeId}`;
    }
    const dataType = dataTypeRefs?.[typeRef.dataTypeId];
    if (!dataType) {
      throw `Could not find data type ref for id: ${typeRef.dataTypeId}`;
    }
    addDescItem(
      createEmptyDescItem(
        typeRef.id,
        nodeId,
        nodeVersionId,
        position,
        dataType.code,
      ),
    );
  }

  async function deleteDescItem(itemId: number) {
    const item = formData?.descItems.find(({ id }) => itemId === id);
    if (!item) {
      throw `Could not find descItem with the id: ${itemId}`;
    }

    if (item.data?.dataId !== undefined || item.undefined) {
      await Api.descItems.descItemDeleteDescItem(fondsVersionId, item);
      return;
    } else {
      // when dataId is missing, item is not saved on server and is only deleted locally
      const index = formData.descItems.findIndex(
        ({ itemTypeId, position, data }) =>
          itemTypeId === item.itemTypeId &&
          position === item.position &&
          data.dataId === undefined,
      );
      const _formData = {
        ...formData,
        descItems: [
          ...formData.descItems.slice(0, index),
          ...formData.descItems.slice(index + 1),
        ],
      };
      setFormData(_formData);
      return;
    }
  }

  return { formData, nodeData, addDescItem, addEmptyDescItem, deleteDescItem };
}

let keymap: Record<string, string> = {};
let counter = 0;

export function useKeyGen(nodeId: number) {
  useEffect(() => {
    keymap = {};
    counter = 0;
  }, [nodeId]);

  function generateKey() {
    const key = `desc-item-${counter}`;
    counter++;
    return key;
  }

  function getKey(_id: number | string) {
    const id = _id.toString();
    const key = keymap[id];
    if (key == undefined) {
      const _key = generateKey();
      keymap[id] = _key;
      return _key;
    } else {
      return key;
    }
  }

  function pairKey(id: number | string, oldKey: string) {
    const oldId = Object.entries(keymap).find(
      ([_key, value]) => value === oldKey,
    )?.[0];
    if (oldId != undefined) {
      // delete keymap[oldId];
      keymap[id] = oldKey;
    }
  }

  return { getKey, pairKey };
}
