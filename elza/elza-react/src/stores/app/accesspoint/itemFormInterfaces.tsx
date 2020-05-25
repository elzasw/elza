import {ApItemExt} from './itemForm';
import {ApFormVO} from "../../../api/ApFormVO";
import {ApStateVO} from "../../../api/ApStateVO";
import {ApFragmentVO} from "../../../api/ApFragmentVO";

export enum DataTypeCode {
    FILE_REF = 'FILE_REF',
    PARTY_REF = 'PARTY_REF',
    RECORD_REF = 'RECORD_REF',
    STRUCTURED = 'STRUCTURED',
    JSON_TABLE = 'JSON_TABLE',
    ENUM = 'ENUM',
    UNITDATE = 'UNITDATE',
    UNITID = 'UNITID',
    FORMATTED_TEXT = 'FORMATTED_TEXT',
    TEXT = 'TEXT',
    STRING = 'STRING',
    INT = 'INT',
    COORDINATES = 'COORDINATES',
    DECIMAL = 'DECIMAL',
    DATE = 'DATE',
    APFRAG_REF = 'APFRAG_REF',
    URI_REF = "URI_REF",
}

export interface ItemFragmentRefVO extends ApItemExt<number> {
    fragment: ApFragmentVO;
}
