import { DaoFileFolderVO } from "./DaoFileFolderVO";
import { DaoVO } from "./DaoVO";

export type DaoFileVO = {
    daoFileId: number;
    dao: DaoVO;
    checksum: string;
    checksumType: string;
    mimeType: string;
    daoFileFolder: DaoFileFolderVO;
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