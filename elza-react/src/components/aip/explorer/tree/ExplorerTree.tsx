import { Tree, TreeItem, TreeItemLayout, TreeItemValue, TreeOpenChangeData, TreeOpenChangeEvent } from "@fluentui/react-components";
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
import { useExplorerContext } from "../ExplorerContext";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { findNodeByUUID } from "../utils";


const AipTree: FC = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const {data: structure} = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const {selectedItem, setSelectedItem, setStructure} = useExplorerContext();

    if (structure) {
        structure.parent = null;
    }

    const [openItems, setOpenItems] = useState<TreeItemValue[]>([]);
    const dispatch = useThunkDispatch();

    const handleOpenChange = (
        event: TreeOpenChangeEvent,
        data: TreeOpenChangeData
    ) => {
        const {node} = findNodeByUUID(structure, data.value);
        setSelectedItem(node);
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
        setSelectedItem(structure);
        setStructure(structure)
    }, [structure]);

    useEffect(() => {
        if (selectedItem) {
            openChange(selectedItem.uuid);
        }
    }, [selectedItem]);

    if(!structure) {
        return <></>
    }

    return (
         <Tree
            aria-label="Průzkumník"
            openItems={openItems}
            onOpenChange={handleOpenChange}
            defaultOpenItems={[structure.uuid]}
            className="explorer-tree"
        >
            {structure && <TreeItem itemType="branch" value={structure.uuid}>
                <TreeItemLayout
                    expandIcon={
                        openItems.includes(structure.uuid) ? 
                            <SubtractSquare16Regular color="black"/> : 
                            <AddSquare16Regular color="black"/>
                    }
                >
                    {structure.label}
                </TreeItemLayout>
                <Tree>
                    {structure?.childFolders && 
                    structure.childFolders.map((folder: DaoFileFolderVO, index: number) => <Folder 
                            key={`root-${index}`}
                            folder={folder} 
                            openItems={openItems} 
                            parent={structure}
                        />
                    )}
                </Tree>
            </TreeItem>}
         </Tree>
    );
}

export default AipTree;

