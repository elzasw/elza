import { TreeItem, TreeItemLayout, Tree, TreeItemValue } from "@fluentui/react-components";
import File from "./File"
import {
    AddSquare16Regular,
    SubtractSquare16Regular,
  } from "@fluentui/react-icons";
import { useExplorerContext } from "../ExplorerContext";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { turncate } from "../utils";

type FolderProps = {
    folder: DaoFileFolderVO;
    openItems: TreeItemValue[];
    parent?: DaoFileFolderVO;
}

const Folder = ({folder, openItems, parent}: FolderProps) => {
    const {selectedItem} = useExplorerContext();
    const isSelected: boolean = selectedItem.uuid == folder.uuid;
    folder.parent = parent;

    const getExpandIcon = () => {
        return openItems.includes(folder.uuid) ? (
            <SubtractSquare16Regular color="black" />
            ) : (
            <AddSquare16Regular color="black"/>
            )
    }

    return (
        <TreeItem itemType="branch" value={folder.uuid} >
            <TreeItemLayout
                style={{backgroundColor: isSelected ?  "#e3e3e3ff" : undefined}}
                expandIcon={getExpandIcon()}
            >
                {turncate(folder.label)}
            </TreeItemLayout>
            <Tree>
                {folder.childFolders?.map((item, index) =>
                    <Folder
                        key={`folder-${index}`}
                        folder={item}
                        openItems={openItems}
                        parent={folder}
                     />
                )}
                {folder.childFiles?.map((file, index) =>
                    <File
                        key={`file-${index}`}
                        file={file}
                        parent={folder}
                    />
                )}
            </Tree>
        </TreeItem>
    );
}
export default Folder;
