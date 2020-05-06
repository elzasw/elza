/**
 * VO Odlehčená verze specifikace hodnoty atributu.
 */
import {DescItemSpecLiteVO} from "./DescItemSpecLiteVO";

export interface ItemTypeLiteVO {
    /**
     * identifikator typu
     */
    id: number;

    /**
     * typ
     */
    type: number;

    /**
     * opakovatelnost
     */
    rep: number;

    /**
     * počítaný
     */
    cal: number;

    /**
     * stav počítanýho atributu
     * - 0 - vypnutý (použije se vypočtená hodnota)
     * - 1 - zapnutý (lze zadat hodnotu manuálně)
     *
     * Číslené z důvodu optimalizace
     */
    calSt: number;

    /**
     * atribut se může nastavit jako nedefinovaný
     * - 0 - nemůže
     * - 1 - může
     */
    ind: number;

    /**
     * seznam specifikací atributu
     */
    specs: DescItemSpecLiteVO[];

    /**
     * seznam identifikátorů oblíbených specifikací u typu
     */
    favoriteSpecIds: number[];

    /**
     * šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců)
     */
    width: number;
}
