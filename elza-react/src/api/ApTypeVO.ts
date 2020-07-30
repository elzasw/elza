import {BaseCodeVo} from "./BaseCodeVo";

export interface ApTypeVO extends BaseCodeVo {
    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    addRecord: boolean;
    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    parentApTypeId: number;
    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     */
    partyTypeId: number;
    /**
     * Seznam potomků.
     */
    children: ApTypeVO[];

    relationRoleTypIds: number;

    /**
     * Seznam rodičů seřazený od přímého rodiče po kořen.
     * @deprecated
     */
    parents: string[];
}
