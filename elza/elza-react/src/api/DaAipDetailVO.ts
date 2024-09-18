import { LinkType } from "elza-api";
import { ParInstitutionVO } from "./ParInstitutionVO.ts";
import { QueueItemState } from "./QueueItemState.ts";
import {LinkedNodeVO} from "./LinkedNodeVO.ts";

export type DaAipDetailVO = {
    aipId: number;
    code: string;
    digitalRepositoryId: number;
    aipVersion: string;
    fund: any;
    institution: ParInstitutionVO;
    institutionCode: string;
    unitdateFrom: string;
    unitdateTo: string;
    originatorInstitution: ParInstitutionVO;
    originator: string;
    ingestionCode: string;
    referenceNumber: string;
    nadChangeCode: string;
    aipSize: number;
    metadataLoad: boolean;
    importState: QueueItemState;
    exportState: QueueItemState;
    arrDaoLinkType: LinkType;
    linkedNodes?: Array<LinkedNodeVO>;
    comleteAipLoad: boolean;
    metadataError: boolean;
    metadataErrorException: string;
    aipVersionMetadata: string;
}
