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


import { AeItemLinkVOAllOf } from './ae-item-link-voall-of';
import { AeItemVO } from './ae-item-vo';

/**
 * Hodnota atributu - odkaz
 * @export
 * @interface AeItemLinkVO
 */
export interface AeItemLinkVO extends AeItemVO {
    /**
     * odkaz
     * @type {string}
     * @memberof AeItemLinkVO
     */
    value: string;
    /**
     * název odkazu
     * @type {string}
     * @memberof AeItemLinkVO
     */
    name?: string;
}


