package cz.tacr.elza.daoimport.service.vo;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class ImportBatch {

    private Path batchDir;
    private String batchName;
    private BufferedWriter protocol;

    private List<ImportDao> daos = new LinkedList();

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

    public BufferedWriter getProtocol() {
        return protocol;
    }

    public void setProtocol(BufferedWriter protocol) {
        this.protocol = protocol;
    }

    public List<ImportDao> getDaos() {
        return daos;
    }
}
