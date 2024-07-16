import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { DaoFileVO } from "api/DaoFileVO";
import { createContext, useContext, useState } from "react";

type TExplorerContext = {
    selectedItem: DaoFileFolderVO | DaoFileVO;
    setSelectedItem: (item: DaoFileFolderVO | DaoFileVO) => void;
}

const ExpContext = createContext<TExplorerContext>(null);

export const useExplorerContext = () => (useContext(ExpContext));

export const isDaoFileFolderVO = (item: DaoFileFolderVO | DaoFileVO): item is DaoFileFolderVO => {
    return (item as DaoFileFolderVO)?.daoFileFolderId !== undefined;
}

type ECProps = {
    children: React.ReactNode;
}


const ExplorerContext = ({children}: ECProps) => {
    const [selectedItem, setSelectedItem] = useState<DaoFileFolderVO | DaoFileVO>(null);
    
    return (
        <ExpContext.Provider value={{
            selectedItem,
            setSelectedItem,
        }}>
            {children}
        </ExpContext.Provider>
    );
};

export default ExplorerContext;


