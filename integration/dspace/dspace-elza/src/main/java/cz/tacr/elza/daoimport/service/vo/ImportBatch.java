package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.daoimport.protocol.Protocol;

public class ImportBatch {

    private Path batchDir;
    private String batchName;
    private Protocol protocol;

    private List<ImportDao> daos = new LinkedList<>();

    public void addDao(ImportDao dao){
        daos.add(dao);
    }

    public Path getBatchDir() {
        return batchDir;
    }

    public void setBatchDir(Path batchDir) {
        this.batchDir = batchDir;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(final Protocol protocol) {
        this.protocol = protocol;
    }

    public List<ImportDao> getDaos() {
        return daos;
    }
}
