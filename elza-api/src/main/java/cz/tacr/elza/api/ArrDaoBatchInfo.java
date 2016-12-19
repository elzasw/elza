package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Přejímka.
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDaoBatchInfo extends Serializable {

    Integer getDaoBatchInfoId();

    void setDaoBatchInfoId(Integer daoBatchInfoId);

    String getCode();

    void setCode(String code);

    String getLabel();

    void setLabel(String label);
}
