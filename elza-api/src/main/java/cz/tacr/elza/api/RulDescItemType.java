package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * evidence typů atributů archivního popisu. evidence je společná pro všechny archivní pomůcky.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RT> {@link RulDataType}
 */
public interface RulDescItemType<RT extends RulDataType> extends Serializable {


    Integer getDescItemTypeId();


    void setDescItemTypeId(final Integer descItemTypeId);


    RT getDataType();


    void setDataType(final RT dataType);

    String getCode();


    void setCode(final String code);


    String getName();


    void setName(final String name);


    String getShortcut();


    void setShortcut(final String shortcut);

    /**
     * @return popis atributu, který slouží zároveň jako nápověda v aplikaci o jaký typ se jedná a jak se sním zachází.
     */
    String getDescription();

    /**
     * popis atributu, který slouží zároveň jako nápověda v aplikaci o jaký typ se jedná a jak se sním zachází.
     * @param description popis atributu.
     */
    void setDescription(final String description);

    /**
     * @return příznak, zda je hodnota atributu při použití tohoto typu jedinečná v rámci celé archivní pomůcky.
     */
    Boolean getIsValueUnique();

    /**
     * příznak, zda je hodnota atributu při použití tohoto typu jedinečná v rámci celé archivní pomůcky.
     * @param isValueUnique příznak.
     */
    void setIsValueUnique(final Boolean isValueUnique);

    /**
     * @return příznak, zda je možné dle tohoto typu atributu setřídit archivní popis. zatím nebude aplikačně využíváno
     */
    Boolean getCanBeOrdered();

    /**
     * nastaví příznak, zda je možné dle tohoto typu atributu setřídit archivní popis.
     * @param canBeOrdered příznak, zda je možné dle tohoto typu atributu setřídit archivní popis.
     */
    void setCanBeOrdered(final Boolean canBeOrdered);

    /**
     * @return příznak, zda se u typu atributu používají specifikace hodnot jako např. u druhů
     *         jednotek popisu nebo u rolí entit. true = povinná specifikace, false = specifikace
     *         neexistují. specifikace jsou uvedeny v číselníku rul_desc_item_spe.
     */
    Boolean getUseSpecification();

    /**
     * příznak, zda se u typu atributu používají specifikace hodnot jako např. u druhů jednotek
     * popisu nebo u rolí entit. true = povinná specifikace, false = specifikace neexistují.
     * specifikace jsou uvedeny v číselníku rul_desc_item_spe.
     *
     * @param useSpecification příznak, zda se u typu atributu používají specifikace hodnot.
     */
    void setUseSpecification(final Boolean useSpecification);

    /**
     * @return pořadí typu atributu pro zobrazení v ui. pokud není pořadí uvedeno nebo je u více
     *         typů uvedeno stejné pořadí, bude výsledné pořadí náhodné.
     */
    Integer getViewOrder();

    /**
     * nastaví pořadí typu atributu pro zobrazení v ui. pokud není pořadí uvedeno nebo je u více
     * typů uvedeno stejné pořadí, bude výsledné pořadí náhodné..
     *
     * @param viewOrder pořadí typu atributu pro zobrazení v ui.
     */
    void setViewOrder(final Integer viewOrder);

}
