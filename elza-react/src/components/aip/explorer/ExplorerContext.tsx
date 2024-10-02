import { TreeItemValue } from "@fluentui/react-tree";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { DaoFileVO } from "api/DaoFileVO";
import { createContext, useContext, useState } from "react";

export enum ExplorerMode {
    VIEW = "view",
    SELECT = "select"
}

type TExplorerContext = {
    selectedItem: any;
    setSelectedItem: (item: any) => void;
    structure: any;
    setStructure: (structure: any) => void;
    mode: ExplorerMode;
    setMode: (mode: ExplorerMode) => void;
}

const ExpContext = createContext<TExplorerContext>(null);

export const useExplorerContext = () => (useContext(ExpContext));

export const isDaoFileFolderVO = (item: DaoFileFolderVO | DaoFileVO): item is DaoFileFolderVO => {
    return (item as DaoFileFolderVO)?.daoFileFolderId !== undefined;
}

type ECProps = {
    mode: ExplorerMode;
    children: React.ReactNode;
}


const ExplorerContext = ({mode: modeProp, children}: ECProps) => {
    const [selectedItem, setSelectedItem] = useState<DaoFileFolderVO | DaoFileVO>(null);
    const [mode, setMode] = useState<ExplorerMode>(modeProp);
    const [structure, setStructure] = useState<DaoFileFolderVO>(null);

    return (
        <ExpContext.Provider value={{
            selectedItem,
            setSelectedItem,
            mode,
            setMode,
            structure,
            setStructure
        }}>
            {children}
        </ExpContext.Provider>
    );
};

export default ExplorerContext;


