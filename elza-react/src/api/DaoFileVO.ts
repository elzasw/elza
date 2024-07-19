import { DaoFileFolderVO } from "./DaoFileFolderVO";
import { DaoVO } from "./DaoVO";

export type DaoFileVO = {
    uuid: string;
    daoFileId: number;
    dao: DaoVO;
    checksum: string;
    checksumType: string;
    mimeType: string;
    daoFileFolder: DaoFileFolderVO;
    parentFolderLogical?: DaoFileFolderVO;
    description?: string;
    duration?: number;
    fileName: string;
    imageHeight?: number;
    imageWifth?: number;
    parent?: DaoFileFolderVO;
    size: number;
    sourceXDimensionUnit?: string;
    sourceXDimensionValue?: number;
    sourceYDimensionUnit?: string;
    sourceYDimensionValue?: number;
}