import { TreeItem, TreeItemLayout, Tree, TreeItemValue } from "@fluentui/react-components";
import File from "./File"
import {
    AddSquare16Regular,
    SubtractSquare16Regular,
  } from "@fluentui/react-icons";
import { isDaoFileFolderVO, useExplorerContext } from "../ExplorerContext";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { turncate } from "../utils";

type FolderProps = {
    folder: DaoFileFolderVO;
    openItems: TreeItemValue[];
    parent?: DaoFileFolderVO;
}

const Folder = ({folder, openItems, parent}: FolderProps) => {
    const {selectedItem} = useExplorerContext();
    const isSelected: boolean = isDaoFileFolderVO(selectedItem) && selectedItem.daoFileFolderId == folder.daoFileFolderId;
    const isLeaf: boolean = (!folder.childFolders && !folder.childFiles) || (folder.childFolders && folder.childFolders.length == 0);
    folder.parent = parent;

    const getExpandIcon = () => {
        if(isLeaf) return undefined;
        return openItems.includes(folder as unknown as TreeItemValue) ? (
            <SubtractSquare16Regular color="black" />
            ) : (
            <AddSquare16Regular color="black"/>
            ) 
    }

    return (
        <TreeItem itemType={isLeaf ? "leaf": "branch"} value={folder as unknown as TreeItemValue} >
            <TreeItemLayout
                style={{backgroundColor: isSelected ?  "#e3e3e3ff" : undefined}}
                expandIcon={getExpandIcon()}
            >
                {turncate(folder.label)}
            </TreeItemLayout>
            <Tree>
                {folder.childFolders?.map(item => <Folder folder={item} openItems={openItems} parent={folder} />)}
                {folder.childFiles?.map(file => <File file={file} parent={folder}/>)}
            </Tree>
        </TreeItem>
    );
}
export default Folder;