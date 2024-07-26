import { QueueItemState } from "./QueueItemState.ts";

export interface DaAipDetailVO {
    aipId: number;
    code: string;
    digitalRepositoryId: number;
    aipVersion: string;
    fundName: string;
    fundCode: string;
    instApName: string;
    institutionCode: string;
    unitdateFrom: string;
    unitdateTo: string;
    originApName: string;
    ingestionCode: string;
    referenceNumber: string;
    nadChangeCode: string;
    aipSize: number;
    metadataLoad: boolean;
    syncState: QueueItemState;
}