import { DaoFileFolderVO } from "./DaoFileFolderVO";
import { DaoVO } from "./DaoVO";
import {LinkedNodeVO} from "./LinkedNodeVO.ts";

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
    filename: string;
    isLogical?: boolean;
    imageHeight?: number;
    imageWifth?: number;
    parent?: DaoFileFolderVO;
    size: number;
    sourceXDimensionUnit?: string;
    sourceXDimensionValue?: number;
    sourceYDimensionUnit?: string;
    sourceYDimensionValue?: number;
    linkedNodes?: LinkedNodeVO[];
}
