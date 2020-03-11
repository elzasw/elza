import { ApFormVO, ApItemExt } from './itemForm';

export enum DataTypeCode {
    FILE_REF = "FILE_REF",
    PARTY_REF = "PARTY_REF",
    RECORD_REF = "RECORD_REF",
    STRUCTURED = "STRUCTURED",
    JSON_TABLE = "JSON_TABLE",
    ENUM = "ENUM",
    UNITDATE = "UNITDATE",
    UNITID = "UNITID",
    FORMATTED_TEXT = "FORMATTED_TEXT",
    TEXT = "TEXT",
    STRING = "STRING",
    INT = "INT",
    COORDINATES = "COORDINATES",
    DECIMAL = "DECIMAL",
    DATE = "DATE",
    APFRAG_REF = "APFRAG_REF",
}

enum ApStateVO {
    OK = "OK",
    ERROR = "ERROR",
    TEMP = "TEMP",
    INIT = "INIT"
}

export interface ApFragmentVO {
    id: number;
    value: string;
    state: ApStateVO;
    typeId: number;
    errorDescription?: string;

    form?: ApFormVO;
}

export interface ItemFragmentRefVO extends ApItemExt<number> {
    fragment: ApFragmentVO;
}
