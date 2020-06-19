/**
 * Oblast vyhledávání:  * `PREFER_NAMES` - pouze preferovaná označení  * `ALL_NAMES` - všechna označení  * `ALL_PARTS` - všechny části popisu AE  * `ENTITY_CODE` - vyhledání podle kódu entity
 * @export
 * @enum {string}
 */
export enum Area {
    PREFERNAMES = 'PREFER_NAMES',
    ALLNAMES = 'ALL_NAMES',
    ALLPARTS = 'ALL_PARTS',
    ENTITYCODE = 'ENTITY_CODE'
}
