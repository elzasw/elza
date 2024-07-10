import {  Tree, TreeItem, TreeItemLayout, TreeItemValue, TreeOpenChangeData, TreeOpenChangeEvent } from "@fluentui/react-components";
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
import { WebApi } from "actions";
import { AREA_EXPLORER_ITEM, setExplorerItem } from "actions/aip/exp";
import "./ExplorerTree.scss"


const AipTree: FC = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const expItem = useSelector((state: AppState) => storeFromArea(state, AREA_EXPLORER_ITEM));
    const [openItems, setOpenItems] = useState<TreeItemValue[]>([]);
    const dispatch = useThunkDispatch();
    const [root, setRoot] = useState(null);

    const handleOpenChange = (
        event: TreeOpenChangeEvent,
        data: TreeOpenChangeData
    ) => {
        dispatch(setExplorerItem(data.value.daoFileFolderId, data.value));
        setOpenItems((curr) =>
            data.open
                ? [...curr, data.value]
                : curr.filter((value) => value !== data.value)
        );
    };

    const fetchData = async () => {
        WebApi.getDaDaoListByAipId(aip.id).then(res => 
            setRoot(res)
        );
    }

    useEffect(() => {
        if(aip.id) {
            fetchData();
        }
    }, [aip.id]);

  
    const renderChildrenFolders = (children) => {
        return (
            <Tree>
                {children.map(item => 
                    <TreeItem itemType={"branch"} value={item} >
                        <TreeItemLayout
                            style={{backgroundColor: expItem?.id == item.daoFileFolderId ? "#e3e3e3ff" : undefined}}
                            expandIcon={
                                openItems.includes(item) ? (
                                <SubtractSquare16Regular color="black" />
                                ) : (
                                <AddSquare16Regular color="black"/>
                                ) 
                            }
                        >
                            {item.label}   
                        </TreeItemLayout>
                        {item.childFolders?.length > 0 && renderChildrenFolders(item.childFolders)}
                        {item.childFiles?.length > 0 && renderChildrenFiles(item.childFiles)}
                    </TreeItem>
                )}
            </Tree>
        );

    }
    const renderChildrenFiles = (files) => {
        return (
            <>
                {files.map(file => 
                    <TreeItem itemType="leaf">
                        <TreeItemLayout>{file.fileName}</TreeItemLayout>
                    </TreeItem>
                )}
            </>
        )
    }
    
    return(
         <Tree
            aria-label="Průzkumník"
            openItems={openItems}
            onOpenChange={handleOpenChange}
        >
            <TreeItem itemType="branch" value="Balíček">
                <TreeItemLayout
                    expandIcon={
                        openItems.includes("Balíček") ? (
                        <SubtractSquare16Regular color="black"/>
                        ) : (
                        <AddSquare16Regular color="black"/>
                        )
                    }
                >
                    Balíček (METS.xml)
                </TreeItemLayout>
                <Tree>
                    <TreeItem itemType="branch" value="Reprezentace">
                        <TreeItemLayout
                            expandIcon={
                                openItems.includes("Reprezentace") ? (
                                <SubtractSquare16Regular color="black" />
                                ) : (
                                <AddSquare16Regular color="black"/>
                                )
                            }
                        >
                            Reprezentace  
                        </TreeItemLayout>
                        {root && 
                            <Tree  
                                // aria-label="Průzkumník"
                                // openItems={openItems}
                                // onOpenChange={handleOpenChange}

                            >
                                <TreeItem itemType="branch" value={root}>
                                    <TreeItemLayout
                                        style={{backgroundColor: expItem?.id == root.daoFileFolderId ? "#e3e3e3ff" : undefined}}
                                        expandIcon={
                                            openItems.includes(root) ? (
                                            <SubtractSquare16Regular color="black" />
                                            ) : (
                                            <AddSquare16Regular color="black"/>
                                            )
                                        }
                                    >
                                        {root.label}   
                                    </TreeItemLayout>
                                    {root.childFolders?.length > 0 && renderChildrenFolders(root.childFolders)}
                                    {/* {root.childFiles?.length > 0 && renderChildrenFiles(root.childFiles)} */}
                                </TreeItem>
                            </Tree>
}
                    </TreeItem>
                    <TreeItem itemType="branch" value="Logická struktura">
                        <TreeItemLayout
                            expandIcon={
                                openItems.includes("Logická struktura") ? (
                                <SubtractSquare16Regular color="black" />
                                ) : (
                                <AddSquare16Regular color="black"/>
                                )
                            }
                        >
                            Logická struktura 
                        </TreeItemLayout>
                            {/* {root.childFolders?.length > 0 && renderChildrenFolders(root.childFolders)} */}
                            {/* {root.childFiles?.length > 0 && renderChildrenFiles(root.childFiles)} */}
                    </TreeItem>
                    <TreeItem itemType="branch" value="Metadata">
                        <TreeItemLayout
                            expandIcon={
                                openItems.includes("Metadata") ? (
                                <SubtractSquare16Regular color="black" />
                                ) : (
                                <AddSquare16Regular color="black"/>
                                )
                            }
                        >
                            Metadata 
                        </TreeItemLayout>
                            {/* {root.childFolders?.length > 0 && renderChildrenFolders(root.childFolders)} */}
                            {/* {root.childFiles?.length > 0 && renderChildrenFiles(root.childFiles)} */}
                    </TreeItem>
                </Tree>
                
              
            </TreeItem>
         </Tree>
    );
}

export default AipTree;

