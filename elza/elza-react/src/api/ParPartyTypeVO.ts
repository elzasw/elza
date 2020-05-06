import {ApTypeVO} from "./ApTypeVO";
import {ParRelationTypeVO} from "./ParRelationTypeVO";
import {ParComplementTypeVO} from "./ParComplementTypeVO";
import {UIPartyGroupVO} from "./UIPartyGroupVO";

export interface ParPartyTypeVO {
    /**
     * Id.
     */
    id: number;

    /**
     * Kod typu osoby.
     */
    code: string;
    /**
     * Název typu osoby.
     */
    name: string;

    /**
     * Popis typu osoby.
     */
    description: string;

    relationTypes: ParRelationTypeVO[];
    complementTypes: ParComplementTypeVO[];
    apTypes: ApTypeVO[];
    partyGroups: UIPartyGroupVO[];
}
