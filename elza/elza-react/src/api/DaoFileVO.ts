import { DaoFileFolderVO } from "./DaoFileFolderVO";
import {LinkedNodeVO} from "elza-api";

export type DaoFileVO = {
    uuid: string;
    daoFileId: number;
    checksum: string;
    checksumType: string;
    mimeType: string;
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
