package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Hodnoty datace.
 *
 * @param <CT> typ kalendáře
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParUnitdate<CT extends ArrCalendarType> extends Serializable, IUnitdate<CT> {

    Integer getUnitdateId();

    void setUnitdateId(Integer unitdateId);

    /**
     * @return Text pokud není validní datace.
     */
    String getTextDate();


    /**
     * @param textDate Text pokud není validní datace.
     */
    void setTextDate(String textDate);
}
