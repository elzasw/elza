package cz.tacr.elza.controller.vo.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.factory.RuleFactory;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemTypeExt;


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

    public ItemTypeLiteVO() {

    }
    
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

    public static ItemTypeLiteVO newInstance(final RulItemTypeExt itemType) {
        ItemTypeLiteVO vo = new ItemTypeLiteVO();
        List<RulItemSpecExt> rulItemSpecs = itemType.getRulItemSpecList();
        List<DescItemSpecLiteVO> specItems = new ArrayList<>();
        for (RulItemSpecExt rulItemSpec : rulItemSpecs) {
            if (rulItemSpec.getType() != RulItemSpec.Type.IMPOSSIBLE) {
                specItems.add(DescItemSpecLiteVO.newInstance(rulItemSpec));
            }
        }

        vo.setId(itemType.getItemTypeId());
        vo.setType(RuleFactory.convertType(itemType.getType()));
        vo.setRep(itemType.getRepeatable() ? 1 : 0);
        vo.setSpecs(specItems);
        vo.setWidth(1); // není zatím nikde definované
        vo.setCalSt(itemType.getCalculableState() ? 1 : 0);
        vo.setCal(itemType.getCalculable() ? 1 : 0);
        vo.setInd(itemType.getIndefinable() ? 1 : 0);
        vo.setFavoriteSpecIds(null); // TODO

        return vo;
    }
}
