import {ApCreateTypeVO} from "./ApCreateTypeVO";

export interface ApAttributesInfoVO {

    /**
     * Vyhodnocené typy a specifikace atributů, které jsou třeba pro založení přístupového bodu
     */
    attributes: ApCreateTypeVO[];

    /**
     * Chyby při validaci
     */
    errors?: string[];
}
