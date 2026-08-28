package com.lightcomp.kads.common;

/**
 * The XML document does not carry the content it was expected to carry - it is not
 * well-formed, it is empty or its root element belongs to a different standard.
 *
 * The message describes the defect of the document in a form that can be shown to the user,
 * so it names what was expected and what the document actually contains.
 */
public class XmlContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public XmlContentException(String message) {
        super(message);
    }

    public XmlContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
