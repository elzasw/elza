import { DaoFileVO } from "./DaoFileVO";
import {LinkedNodeVO} from "elza-api";

export type DaoFileFolderVO = {
    uuid: string;
    daoFileFolderId: number;
    createChange?: string;
    deleteChange?: string;
    label: string;
    childFiles?: DaoFileVO[];
    childFolders?: DaoFileFolderVO[];
    parent?: DaoFileFolderVO;
    parentFolder?: DaoFileFolderVO;
    parentFolderLogical?: DaoFileFolderVO;
    linkedNodes?: LinkedNodeVO[];
}
