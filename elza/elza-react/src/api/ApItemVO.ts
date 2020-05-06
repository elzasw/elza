export interface ApItemVO {
    /**
     * Discriminator
     * @type {string}
     * @memberof AeItemVO
     */
    '@class': string;

    /**
     * identifikátor
     */
    id: number;

    /**
     * identifikátor hodnoty atributu
     */
    objectId: number;

    /**
     * pozice
     */
    position: number;

    /**
     * typ atributu
     */
    typeId: number;

    /**
     * specifikace atributu
     */
    specId: number;
}
