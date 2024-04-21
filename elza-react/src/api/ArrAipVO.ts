import {DipType} from "./DipType.ts";
import {ProcessState} from "./ProcessState.ts";

export interface ArrAipVO {

    id: number;

    aipId: number;

    extAipId: number;

    name: string;

    aipVersion: number;

    aipSize: number;

    fundId: number;

    fundName: string;

    institutionId: number;

    institutionName: string;

    createDate: string;

    dipType: DipType;

    processState: ProcessState;

    syncDate: string;
}
