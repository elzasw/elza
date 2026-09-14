import { DaoFileVO } from "./DaoFileVO";
import {AipLevelType, LinkedNodeVO} from "elza-api";

export type DaoFileFolderVO = {
    uuid: string;
    daoFileFolderId: number;
    createChange?: string;
    deleteChange?: string;
    label: string;
    /** Typ virtuální úrovně; reálné složky a soubory ho nemají. */
    levelType?: AipLevelType;
    childFiles?: DaoFileVO[];
    childFolders?: DaoFileFolderVO[];
    parent?: DaoFileFolderVO;
    parentFolder?: DaoFileFolderVO;
    parentFolderLogical?: DaoFileFolderVO;
    linkedNodes?: LinkedNodeVO[];
}
