import {RequiredType} from "./RequiredType";

export interface ApCreateTypeVO {
    /**
     * Identifikátor typu atributu
     */
    itemTypeId: number;

    /**
     * RequiredType
     */
    requiredType: RequiredType;

    /**
     * Seznam povolených specifikací typu atributu
     */
    itemSpecIds?: number[];

    /**
     * Jedná se o opakovatelný typ prvku popisu?
     */
    repeatable: boolean;
}
