export interface ApItemVO {
    "@class": string;

    /**
     * identifikátor
     */
    id?: number;

    /**
     * identifikátor hodnoty atributu
     */
    objectId?: number;

    /**
     * pozice
     */
    position?: number;

    /**
     * typ atributu
     */
    typeId: number;

    /**
     * specifikace atributu
     */
    specId?: number;

    /**
     * identifikátor původní hodnoty atributu
     */
    origObjectId?: number;
}
