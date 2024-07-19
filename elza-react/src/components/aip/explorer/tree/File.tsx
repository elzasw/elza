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
    const isSelected = selectedItem && !isDaoFileFolderVO(selectedItem) && selectedItem.uuid == file.uuid;
    file.parent = parent;
    
    return (
        <TreeItem
            itemType="leaf" 
            value={file.uuid}
            style={{backgroundColor: isSelected ? "#e3e3e3ff" : undefined}}
        >
            <TreeItemLayout>{turncate(getFileName(file.fileName))}</TreeItemLayout>
        </TreeItem>
    );
}


export default File;