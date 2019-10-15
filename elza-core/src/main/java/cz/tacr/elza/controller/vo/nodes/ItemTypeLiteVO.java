package cz.tacr.elza.controller.vo.nodes;

import java.util.List;


/**
 * VO Odlehčená verze specifikace hodnoty atributu.
 *
 * @author Martin Šlapa
 * @since 11.2.2016
 */
public class ItemTypeLiteVO {

    /**
     * identifikator typu
     */
    private Integer id;

    /**
     * typ
     */
    private Integer type;

    /**
     * opakovatelnost
     */
    private Integer rep;

    /**
     * počítaný
     */
    private Integer cal;

    /**
     * stav počítanýho atributu
     * - 0 - vypnutý (použije se vypočtená hodnota)
     * - 1 - zapnutý (lze zadat hodnotu manuálně)
     *
     * Číslené z důvodu optimalizace
     */
    private Integer calSt;

    /**
     * atribut se může nastavit jako nedefinovaný
     * - 0 - nemůže
     * - 1 - může
     */
    private Integer ind;

    /**
     * seznam specifikací atributu
     */
    private List<DescItemSpecLiteVO> specs;

    /**
     * seznam identifikátorů oblíbených specifikací u typu
     */
    private List<Integer> favoriteSpecIds;

    /**
     * šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců)
     */
    private Integer width;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(final Integer type) {
        this.type = type;
    }

    public Integer getRep() {
        return rep;
    }

    public void setRep(final Integer rep) {
        this.rep = rep;
    }

    public List<DescItemSpecLiteVO> getSpecs() {
        return specs;
    }

    public void setSpecs(final List<DescItemSpecLiteVO> specs) {
        this.specs = specs;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    public Integer getCal() {
        return cal;
    }

    public Integer getCalSt() {
        return calSt;
    }

    public void setCalSt(final Integer calSt) {
        this.calSt = calSt;
    }

    public void setCal(final Integer cal) {
        this.cal = cal;
    }

    public Integer getInd() {
        return ind;
    }

    public void setInd(final Integer ind) {
        this.ind = ind;
    }

    public List<Integer> getFavoriteSpecIds() {
        return favoriteSpecIds;
    }

    public void setFavoriteSpecIds(final List<Integer> favoriteSpecIds) {
        this.favoriteSpecIds = favoriteSpecIds;
    }
}
