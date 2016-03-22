package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * evidence možných datových typů atributů archivního popisu. 
 * evidence je společná pro všechny archivní pomůcky.
 * 
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDataType extends Serializable {


    Integer getDataTypeId();


    void setDataTypeId(final Integer dataTypeId);


    String getCode();


    void setCode(final String code);


    String getName();


    void setName(final String name);


    String getDescription();


    void setDescription(final String description);


    /**
     * @return příznak, zda je možná u datového typu kontrola na regulární výraz.
     */
    Boolean getRegexpUse();


    /**
     * @param regexpUse příznak, zda je možná u datového typu kontrola na regulární výraz.
     */
    void setRegexpUse(final Boolean regexpUse);


    /**
     * @return příznak, zda je možná u datového typu kontrola na maximální možnou délku textového řetězce.
     */
    Boolean getTextLengthLimitUse();


    /**
     * @param textLengthLimitUse příznak, zda je možná u datového typu kontrola na maximální možnou délku textového řetězce.
     */
    void setTextLengthLimitUse(final Boolean textLengthLimitUse);


    /**
     * @return informace, kde je ulozena hodnota (arr_data_xxx).
     */
    String getStorageTable();


    /**
     * @param storageTable informace, kde je ulozena hodnota (arr_data_xxx).
     */
    void setStorageTable(final String storageTable);


}
