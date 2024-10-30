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
    const files = folder?.childFiles?.filter(file => !file.isLogical);
    const isLast = folder?.childFiles?.every(item => item.isLogical) && folder?.childFolders?.length == 0;

    const getExpandIcon = () => {
        return openItems.includes(folder.uuid) ? (
            <SubtractSquare16Regular color="black" />
            ) : (
            <AddSquare16Regular color="black"/>
            )
    }

    return (
        <TreeItem itemType={isLast ? "leaf" : "branch"} value={folder.uuid} >
            <TreeItemLayout
                style={{backgroundColor: isSelected ?  "#e3e3e3ff" : undefined}}
                expandIcon={isLast ? undefined : getExpandIcon()}
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
                {files?.map((file, index) =>
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
