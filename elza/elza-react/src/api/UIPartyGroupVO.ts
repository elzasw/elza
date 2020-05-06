import {ParPartyTypeVO} from "./ParPartyTypeVO";
import {UIPartyGroupTypeEnum} from "./UIPartyGroupTypeEnum";

/**
 * VO nastavení zobrazení formuláře pro osoby.
 */
export interface UIPartyGroupVO {
    id: number;
    partyType: ParPartyTypeVO;
    code: string;
    name: string;
    viewOrder: number;
    type: UIPartyGroupTypeEnum;
    contentDefinition: string;
}
