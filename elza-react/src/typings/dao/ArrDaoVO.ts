import {ArrDaoLinkVO} from "./ArrDaoLinkVO";

export interface ArrDaoVO {
    code?: string;
    daoLink: ArrDaoLinkVO;
    daoType?: string;
    existInArrDaoRequest?: boolean | null;
    fileCount: number;
    fileGroupCount?: number;
    fileGroupList?: any[];
    fileList?: any[];
    id: number;
    label?: string;
    url?: string;
    valid?: boolean;
    scenarios?: string[];
}
