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


import { AttType } from './att-type';
import { ConditionType } from './condition-type';

/**
 * Vyhledávací podmínka pro interpi
 * @export
 * @interface InterpiConditionVO
 */
export interface InterpiConditionVO {
    /**
     *
     * @type {AttType}
     * @memberof InterpiConditionVO
     */
    attType?: AttType;
    /**
     *
     * @type {ConditionType}
     * @memberof InterpiConditionVO
     */
    conditionType?: ConditionType;
    /**
     * vyhledávaný výraz
     * @type {string}
     * @memberof InterpiConditionVO
     */
    value?: string;
}


