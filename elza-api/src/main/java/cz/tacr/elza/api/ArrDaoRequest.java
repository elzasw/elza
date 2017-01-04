package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Dotaz pro externí systémy - DAO / delimitace nebo skartace.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrDaoRequest<DF extends ArrDigitalRepository> extends Serializable {

    DF getDigitalRepository();

    void setDigitalRepository(DF digitalRepository);

    Type getType();

    void setType(Type type);

    String getDescription();

    void setDescription(String description);

    enum Type {

        /**
         * Skartace.
         */
        DESTRUCTION,

        /**
         * Delimitace.
         */
        TRANSFER

    }

}
