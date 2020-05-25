import {ApStateVO} from "./ApStateVO";
import {ApFormVO} from "./ApFormVO";

export interface ApAccessPointNameVO {
    /**
     * Identifikátor záznamu
     */
    id: number;

    /**
     * Id rejstříkového hesla.
     */
    accessPointId: number;

    /**
     * Identifikátor jmena (nemění se při odverování)
     */
    objectId: number;

    /**
     * Název.
     */
    name?: string;

    /**
     * Doplněk.
     */
    complement?: string;

    /**
     * Celé jméno.
     */
    fullName?: string;

    /**
     * Jedná se o preferované jméno?
     */
    preferredName: boolean;

    /**
     * Kód jazyku jména.
     */
    languageCode?: string;

    /**
     * Stav jména.
     */
    state?: ApStateVO;

    /**
     * Chyby ve jméně.
     */
    errorDescription?: string;

    /**
     * Strukturované data formuláře pro jméno. Vyplněné pouze v případě, že se jedná o strukturovaný typ a že se jedná o editační detail.
     */
    form?: ApFormVO;
}
