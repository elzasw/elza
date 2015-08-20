package cz.tacr.elza.api;

import java.io.Serializable;


/**
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


    Boolean getRegexpUse();


    void setRegexpUse(final Boolean regexpUse);


    Boolean getTextLenghtLimitUse();


    void setTextLenghtLimitUse(final Boolean textLenghtLimitUse);


    String getStorageTable();


    void setStorageTable(final String storageTable);


}
