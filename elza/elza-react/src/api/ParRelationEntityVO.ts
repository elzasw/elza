import {ApAccessPointVO} from "./ApAccessPointVO";
import {ParRelationRoleTypeVO} from "./ParRelationRoleTypeVO";

export interface ParRelationEntityVO {
    id: number;
    relationId: number;

    /**
     * Rejstříkové heslo.
     */
    record: ApAccessPointVO;

    /**
     * Typ vztahu.
     */
    roleType: ParRelationRoleTypeVO;

    note: string;
}
