import {ParRelationRoleTypeVO} from "./ParRelationRoleTypeVO";
import {ParRelationClassTypeVO} from "./ParRelationClassTypeVO";
import {UseUnitdateEnum} from "./UseUnitdateEnum";

/**
 * VO pro typů vztahu.
 */
export interface ParRelationTypeVO {
    /**
     * Id.
     */
    id: number;
    /**
     * Název.
     */
    name: string;
    /**
     * Kod.
     */
    code: string;
    /**
     * Typ třídy.
     */
    relationClassType: ParRelationClassTypeVO;

    relationRoleTypes: ParRelationRoleTypeVO[];

    /**
     * Způsob použití datace.
     */
    useUnitdate: UseUnitdateEnum;
}
