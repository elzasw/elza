package cz.tacr.elza.ws;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;


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
