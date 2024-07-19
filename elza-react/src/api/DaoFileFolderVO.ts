import { DaoFileVO } from "./DaoFileVO";
import { DaoVO } from "./DaoVO";

export type DaoFileFolderVO = {
    uuid: string;
    daoFileFolderId: number;
    createChange?: string;
    deleteChange?: string;
    label: string;
    representationDao: DaoVO;
    childFiles?: DaoFileVO[];
    childFolders?: DaoFileFolderVO[];
    parent?: DaoFileFolderVO;
    parentFolder?: DaoFileFolderVO;
    parentFolderLogical?: DaoFileFolderVO;
}