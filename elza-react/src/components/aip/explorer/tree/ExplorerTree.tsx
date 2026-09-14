import { Tree, TreeItem, TreeItemLayout, TreeItemValue, TreeOpenChangeData, TreeOpenChangeEvent } from "@fluentui/react-components";
import type { ExplorerNode } from "../utils";
import { FC,  useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {
    AddSquare16Regular,
    SubtractSquare16Regular,
  } from "@fluentui/react-icons";
import { AREA_AIP } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import { AREA_AIP_STRUCTURE, fetchAipStructureIfNeeded } from "actions/aip/exp";
import "./ExplorerTree.scss"
import Folder from "./Folder";
import { ExplorerMode, useExplorerContext } from "../ExplorerContext";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { findNodeByUUID } from "../utils";
import { levelIcon, useNodeName } from "../levels";
import { useIntl } from "react-intl";
import { explorerMessages } from "../../messages";


const AipTree: FC<{onSelect?: (node: ExplorerNode) => void}> = ({onSelect}) => {
    const intl = useIntl();
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const {data: structure} = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const {selectedItem, setSelectedItem, mode, hideRoot} = useExplorerContext();
    const nodeName = useNodeName();

    if (structure) {
        structure.parent = null;
    }

    const [openItems, setOpenItems] = useState<TreeItemValue[]>([]);
    const dispatch = useThunkDispatch();

    const handleOpenChange = (
        event: TreeOpenChangeEvent,
        data: TreeOpenChangeData
    ) => {
        const result = findNodeByUUID(structure, data.value);
        if(!result) return;
        setSelectedItem(result.node);
        setOpenItems((curr) =>
                data.open
                    ? [...curr, data.value]
                    : curr.filter((value) => value !== data.value)
            )
    };

    const openChange = (value: TreeItemValue, close = false) => {
        const opened = [...openItems];
        if (close) {
            setOpenItems(prev => prev.filter(uuid => uuid !== value))
        } else {
            const result = findNodeByUUID(structure, value);
            if(result) {
                const items = result.path.map(node => node.uuid);
                items.forEach(item => {
                    if (!opened.includes(item)) {
                        opened.push(item)
                    }
                });
                setOpenItems(opened);
            }
        }
    }

    useEffect(() => {
        dispatch(fetchAipStructureIfNeeded(aip.id));
    }, [aip.id]);

    useEffect(() => {
        if (selectedItem) {
            openChange(selectedItem.uuid);
            if(mode == ExplorerMode.SELECT && onSelect) {
                onSelect(selectedItem);
            }
        }
    }, [selectedItem]);

    if(!structure) {
        return <></>
    }

    const sections = structure.childFolders?.map((folder: DaoFileFolderVO, index: number) => <Folder
        key={`root-${index}`}
        folder={folder}
        openItems={openItems}
        parent={structure}
    />);

    if (hideRoot) {
        return (
            <Tree
                aria-label={intl.formatMessage(explorerMessages.treeLabel)}
                openItems={openItems}
                onOpenChange={handleOpenChange}
                className="explorer-tree"
            >
                {sections}
            </Tree>
        );
    }

    return (
         <Tree
            aria-label={intl.formatMessage(explorerMessages.treeLabel)}
            openItems={openItems}
            onOpenChange={handleOpenChange}
            defaultOpenItems={[structure.uuid]}
            className="explorer-tree"
        >
            <TreeItem itemType="branch" value={structure.uuid}>
                <TreeItemLayout
                    expandIcon={
                        openItems.includes(structure.uuid) ?
                            <SubtractSquare16Regular color="black"/> :
                            <AddSquare16Regular color="black"/>
                    }
                    iconBefore={levelIcon(structure.levelType)}
                >
                    {nodeName(structure)}
                </TreeItemLayout>
                <Tree>{sections}</Tree>
            </TreeItem>
         </Tree>
    );
}

export default AipTree;

