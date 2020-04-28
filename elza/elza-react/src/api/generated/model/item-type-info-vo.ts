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


import { PartType } from './part-type';

/**
 * Další informace o typu hodnoty
 * @export
 * @interface ItemTypeInfoVO
 */
export interface ItemTypeInfoVO {
    /**
     * Pořadí typu hodnoty v partu
     * @type {number}
     * @memberof ItemTypeInfoVO
     */
    position?: number;
    /**
     * Šířka typu hodnoty
     * @type {number}
     * @memberof ItemTypeInfoVO
     */
    width: number;
    /**
     *
     * @type {PartType}
     * @memberof ItemTypeInfoVO
     */
    part: PartType;
    /**
     * Příznak pro načítání interpi
     * @type {boolean}
     * @memberof ItemTypeInfoVO
     */
    calc?: boolean;
    /**
     * item type pro interpi
     * @type {number}
     * @memberof ItemTypeInfoVO
     */
    interpiItemTypeId?: number;
    /**
     * interpi item spec
     * @type {number}
     * @memberof ItemTypeInfoVO
     */
    interpiItemSpecId?: number;
    /**
     * příznak pro hierarchii geografických AE
     * @type {string}
     * @memberof ItemTypeInfoVO
     */
    geoSearchItemType?: string;
}


