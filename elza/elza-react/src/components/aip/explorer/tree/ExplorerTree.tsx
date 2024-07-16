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
import { DaoFileVO } from "api/DaoFileVO";
import { turncate } from "../utils";


const AipTree: FC = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const {data: structure} = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const {selectedItem, setSelectedItem} = useExplorerContext();

    if(structure) {
        structure.parent = null;
    }

    const [openItems, setOpenItems] = useState<TreeItemValue[]>([]);
    const dispatch = useThunkDispatch();

    const handleOpenChange = (
        event: TreeOpenChangeEvent,
        data: TreeOpenChangeData
    ) => {
        setSelectedItem(data.value as unknown as (DaoFileFolderVO | DaoFileVO))
        setOpenItems((curr) =>
            data.open
                ? [...curr, data.value]
                : curr.filter((value) => value !== data.value)
        );
    };

    useEffect(() => {
        dispatch(fetchAipStructureIfNeeded(aip.id));
    }, [aip.id]);

    useEffect(() => {
        setSelectedItem(structure);
        setOpenItems([structure]);
    }, [structure]);

    useEffect(() => {
        setOpenItems([...openItems, selectedItem] as TreeItemValue[]);
    }, [selectedItem]);

    return (
         <Tree
            aria-label="Průzkumník"
            openItems={openItems}
            onOpenChange={handleOpenChange}
            style={{overflowX: "auto"}}
            defaultOpenItems={[structure]}
        >
            {structure && <TreeItem itemType="branch" value={structure}>
                <TreeItemLayout
                    expandIcon={
                        openItems.includes(structure) ? 
                            <SubtractSquare16Regular color="black"/> : 
                            <AddSquare16Regular color="black"/>
                    }
                >
                    {turncate(structure.label)}
                </TreeItemLayout>
                <Tree>
                    {structure?.childFolders && 
                    structure.childFolders.map(folder => <Folder 
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

