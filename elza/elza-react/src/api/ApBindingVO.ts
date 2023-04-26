import {SyncState} from "./SyncState";
import {ApBindingItemVO} from "./ApBindingItemVO";

export enum SyncProgress {
    UPLOAD_PENDING = "UPLOAD_PENDING",
    UPLOAD_STARTED = "UPLOAD_STARTED",
}

export interface ApBindingVO {

    id: number;

    externalSystemId: number;

    externalSystemCode: string; // TODO odstranit po dokončení všech změn

    value: string;

    detailUrl: string;

    extState: string;

    extRevision: string;

    extUser: string;

    extReplacedBy?: string;

    detailUrlExtReplacedBy: string;

    syncProgress: SyncProgress;

    syncState: SyncState;

    bindingItemList: ApBindingItemVO[];

}
