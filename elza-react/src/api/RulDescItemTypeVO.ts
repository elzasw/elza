import {RulItemTypeType} from "./RulItemTypeType";
import {TreeItemSpecsItem} from "./TreeItemSpecsItem";

/**
 * VO typu hodnoty atributu
 */
export interface RulDescItemTypeVO {
    /**
     * identifikátor typu
     */
    id: number;

    /**
     * identifikátor datového typu
     */
    dataTypeId: number;

    /**
     * kód
     */
    code: string;

    /**
     * název
     */
    name: string;

    /**
     * zkratka
     */
    shortcut: string;

    /**
     * popis
     */
    description: string;

    /**
     * je hodnota unikátní?
     */
    isValueUnique: boolean;

    /**
     * může se řadit?
     */
    canBeOrdered: boolean;

    /**
     * použít specifikaci?
     */
    useSpecification: boolean;

    /**
     * řazení ve formuláři jednotky popisu
     */
    viewOrder: number;

    /**
     * typ důležitosti
     * @deprecated
     */
    type: RulItemTypeType;

    /**
     * opakovatelnost
     * @deprecated
     */
    repeatable: boolean;

    viewDefinition: object;

    /**
     * Kategorie specifikací.
     */
    itemSpecsTree: TreeItemSpecsItem[];

    /**
     * šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců)
     */
    width: number;

    /**
     * identifikátor strukturovaného typu
     */
    structureTypeId: number;

}
