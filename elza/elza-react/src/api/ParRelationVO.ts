import {ParUnitdateVO} from "./ParUnitdateVO";
import {ParRelationEntityVO} from "./ParRelationEntityVO";

export interface ParRelationVO {
    id: number;
    relationTypeId: number;
    from: ParUnitdateVO;
    to: ParUnitdateVO;
    note: string;
    partyId: number;
    version: number;
    displayName: string;
    source: string;
    relationEntities: ParRelationEntityVO[];
}
