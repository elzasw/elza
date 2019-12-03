package cz.tacr.elza.dataexchange.output.sections;

import java.io.InputStream;
import java.util.function.Supplier;

import cz.tacr.elza.dataexchange.output.writer.FileInfo;

public class DmsFileInfoImpl implements FileInfo {

    private final Integer fileId;
    private final String name;
    private final String fileName;
    private final String mimetype;
    private final Supplier<InputStream> isFactory;

    public DmsFileInfoImpl(final Integer fileId, final String name,
                           final String fileName,
                           final String mimetype,
                           final Supplier<InputStream> isFactory) {
        this.fileId = fileId;
        this.name = name;
        this.fileName = fileName;
        this.mimetype = mimetype;
        this.isFactory = isFactory;
    }

    @Override
    public Integer getId() {
        return fileId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream getInputStream() {
        return isFactory.get();
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMimetype() {
        return mimetype;
    }

}
