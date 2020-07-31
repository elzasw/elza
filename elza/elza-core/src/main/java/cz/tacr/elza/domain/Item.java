package cz.tacr.elza.domain;

/**
 * Sjednocující interface pro {@link ApItem} a {@link ArrItem}.
 */
public interface Item {

    /**
     * @return interní identifikátor položky
     */
    Integer getItemId();

    /**
     * @return identifikátor typu atributu
     */
    Integer getItemTypeId();

    /**
     * @return identifikátor specifikace atributu
     */
    Integer getItemSpecId();

    /**
     * @return pozice hodnoty atributu
     */
    Integer getPosition();

    /**
     * @return data hodnoty atributu
     */
    ArrData getData();

    /**
     * @return jedná se o nedefinovaný atribut (tzn. nemá data hodnoty atributu)
     */
    boolean isUndefined();

}
