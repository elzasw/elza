import {AipType} from "./AipType.ts";

export interface DaAipVO {

    id: number;

    aipId: number;

    code: string;

    aipVersion: string;

    aipSize: number;

    digitalRepositoryId: number;

    aipType: AipType;

    remoteAipId: number;

    fundId: number;

    createDate: string;

    lastChange: string;

    unitdateFrom: string;

    unitdateTo: string;

    originatorCode: string;

    originatorId: number;

    ingestionCode: string;

    refNo: string;

    changeCode: string;
}
