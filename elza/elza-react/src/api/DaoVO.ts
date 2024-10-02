import { DaDaoType } from "elza-api";

export type DaoVO = {
    aip?: number;
    code: string;
    daoId: number;
    files: any[];
    folders: any[];
    label: string;
    type: DaDaoType;
}
