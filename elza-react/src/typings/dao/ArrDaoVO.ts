import {ArrDaoFileVO} from "./ArrDaoFileVO";
import {ArrDaoLinkVO} from "./ArrDaoLinkVO";

export interface ArrDaoVO {
    code?: string;
    daoLink: ArrDaoLinkVO;
    daoType?: string;
    existInArrDaoRequest?: boolean | null;
    fileCount: number;
    fileList?: ArrDaoFileVO[];
    truncated?: boolean;
    id: number;
    label?: string;
    url?: string;
    valid?: boolean;
    scenarios?: string[];
}
