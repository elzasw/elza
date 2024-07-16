import { DaDaoType } from "./DaDaoType";

export type DaoVO = {
    aip?: number;
    code: string;
    daoId: number;
    files: any[];
    folders: any[];
    label: string;
    type: DaDaoType;
}