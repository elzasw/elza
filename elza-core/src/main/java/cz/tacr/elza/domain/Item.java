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
     * @return identifikátor specifikace atributů
     */
    Integer getItemSpecId();

    /**
     * @return pozice hodnoty atributů
     */
    Integer getPosition();

    /**
     * @return data hodnoty atributů
     */
    ArrData getData();

    /**
     * @return id hodnoty atributů
     */
    Integer getDataId();

    /**
     * @return jedná se o nedefinovaný atribut (tzn. nemá data hodnoty atributu)
     */
    boolean isUndefined();

}
