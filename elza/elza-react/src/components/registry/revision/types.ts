import { ApPartVO } from 'api/ApPartVO';
import { ApItemVO } from 'api/ApItemVO';

export interface RevisionPart {
    part?: ApPartVO;
    updatedPart?: ApPartVO;
}

export interface RevisionItem {
    item?: ApItemVO;
    updatedItem?: ApItemVO;
}
