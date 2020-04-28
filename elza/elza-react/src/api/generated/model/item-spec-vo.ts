// tslint:disable
/**
 * CLIENT API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */



/**
 * Specifikace hodnoty atributu
 * @export
 * @interface ItemSpecVO
 */
export interface ItemSpecVO {
    /**
     * Identifikátor
     * @type {number}
     * @memberof ItemSpecVO
     */
    id: number;
    /**
     * Kód
     * @type {string}
     * @memberof ItemSpecVO
     */
    code: string;
    /**
     * Název specifikace
     * @type {string}
     * @memberof ItemSpecVO
     */
    name: string;
    /**
     * Popis specifikace
     * @type {string}
     * @memberof ItemSpecVO
     */
    description?: string;
    /**
     * Zkratka názvu specifikace
     * @type {string}
     * @memberof ItemSpecVO
     */
    shortcut: string;
    /**
     * Seznam navázaných typů hodnot atributu
     * @type {Array<number>}
     * @memberof ItemSpecVO
     */
    itemTypeIds?: Array<number>;
}


