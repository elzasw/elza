import { ApPartVO } from 'api/ApPartVO';
import { ApItemVO } from 'api/ApItemVO';

export interface RevisionPart {
    part?: ApPartVO;
    updatedPart?: ApPartVO;
}

export interface RevisionItem<T extends ApItemVO = ApItemVO> {
    item?: T;
    updatedItem?: T;
    typeId: number;
    "@class": string;
}
