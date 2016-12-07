package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dotaz pro externí systémy.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrRequest<F extends ArrFund> extends Serializable {

    Integer getRequestId();

    void setRequestId(Integer requestId);

    F getFund();

    void setFund(F fund);

    String getCode();

    void setCode(String code);

    State getState();

    void setState(State state);

    LocalDateTime getResponseExternalSystem();

    void setResponseExternalSystem(LocalDateTime responseExternalSystem);

    String getRejectReason();

    void setRejectReason(String rejectReason);

    LocalDateTime getCreate();

    void setCreate(LocalDateTime create);

    enum State {

        /**
         * V přípravě.
         */
        OPEN,

        /**
         * Ve frontě.
         */
        QUEUED,

        /**
         * Odeslán.
         */
        SEND,

        /**
         * Přijat.
         */
        ACCEPTED,

        /**
         * Zamítnut.
         */
        REJECTED

    }

}
