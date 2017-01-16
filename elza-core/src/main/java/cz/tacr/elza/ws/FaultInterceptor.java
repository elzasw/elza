package cz.tacr.elza.ws;

import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.message.Message;

/**
 * Ošetření chybových stavů při poskytování WS
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 11.1.17
 */
public class FaultInterceptor extends FaultOutInterceptor {

    public void handleMessage(Message message) {
        message.put(Message.RESPONSE_CODE, 500);
    }

    public void handleFault(Message message) {
    }
}
