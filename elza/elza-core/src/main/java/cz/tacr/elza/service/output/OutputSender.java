package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrOutput;

/**
 * Interface for sending output to other system
 *
 */
public interface OutputSender {

    /**
     * Send output
     * 
     * @param output
     */
    void send(ArrOutput output);

}
