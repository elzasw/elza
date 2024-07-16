import { TreeItem, TreeItemLayout, TreeItemValue } from "@fluentui/react-components";
import { getFileName, turncate } from "../utils";
import { isDaoFileFolderVO, useExplorerContext } from "../ExplorerContext";
import { DaoFileVO } from "api/DaoFileVO";

type FileProps = {
    file: DaoFileVO;
    parent: any;
}

const File = ({file, parent}: FileProps) => {
    const {selectedItem} = useExplorerContext();
    const isSelected = !isDaoFileFolderVO(selectedItem) && selectedItem.daoFileId == file.daoFileId;
    file.parent = parent;
    
    return (
        <TreeItem
            itemType="leaf" 
            value={file as unknown as TreeItemValue}
            style={{backgroundColor: isSelected ? "#e3e3e3ff" : undefined}}
        >
            <TreeItemLayout>{turncate(getFileName(file.fileName))}</TreeItemLayout>
        </TreeItem>
    );
}


export default File;