package cz.tacr.elza.service;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.stream.StreamResult;

/**
 * Rozšíření {@link StreamResult} o práci s {@link ByteArrayOutputStream}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2015
 */
public class ByteStreamResult extends StreamResult {

    private ByteArrayOutputStream outputStream;

    public ByteStreamResult(ByteArrayOutputStream outputStream) {
        super(outputStream);

        this.outputStream = outputStream;
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }
}
