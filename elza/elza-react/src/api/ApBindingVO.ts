import {SyncState} from "./SyncState";
import {ApBindingItemVO} from "./ApBindingItemVO";

export interface ApBindingVO {

    id: number;

    externalSystemCode: string;

    value: string;

    detailUrl: string;

    extState: string;

    extRevision: string;

    extUser: string;

    extReplacedBy?: string;

    detailUrlExtReplacedBy: string;

    syncState: SyncState;

    bindingItemList: ApBindingItemVO[];

}
