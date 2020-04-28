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


import { AeState } from './ae-state';
import { UserVO } from './user-vo';

/**
 *
 * @export
 * @interface AeDetailHeadLocalVOAllOf
 */
export interface AeDetailHeadLocalVOAllOf {
    /**
     * Lokální klientský identifikátor entity
     * @type {number}
     * @memberof AeDetailHeadLocalVOAllOf
     */
    id?: number;
    /**
     * Globální identifikátor entity
     * @type {number}
     * @memberof AeDetailHeadLocalVOAllOf
     */
    globalId?: number;
    /**
     * Počet komentářů u archivní entity
     * @type {number}
     * @memberof AeDetailHeadLocalVOAllOf
     */
    comments?: number;
    /**
     *
     * @type {UserVO}
     * @memberof AeDetailHeadLocalVOAllOf
     */
    ownerUser?: UserVO;
    /**
     *
     * @type {AeState}
     * @memberof AeDetailHeadLocalVOAllOf
     */
    sourceState?: AeState;
}


