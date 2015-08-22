package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecord<RT extends RegRegisterType, ES extends RegExternalSource> extends Serializable {

    Integer getRecordId();

    void setRecordId(Integer recordId);

    RT getRegisterType();

    void setRegisterType(RT registerType);

    ES getExternalSource();

    void setExternalSource(ES externalSource);

    String getRecord();

    void setRecord(String record);

    String getCharacteristics();

    void setCharacteristics(String characteristics);

    String getComment();

    void setComment(String comment);

    Boolean getLocal();

    void setLocal(Boolean local);

    String getExternal_id();

    void setExternal_id(String external_id);
}
