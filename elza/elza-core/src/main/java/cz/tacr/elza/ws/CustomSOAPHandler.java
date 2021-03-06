package cz.tacr.elza.ws;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class CustomSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // continue with other handler
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // continue with other handler
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // nop
    }

    @Override
    public Set<QName> getHeaders() {
        // understand security
        final QName securityHeader = new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "Security",
                "wsse");
        final Set<QName> headers = new HashSet<>();
        headers.add(securityHeader);

        // notify the runtime that this is handled
        return headers;
    }

}
