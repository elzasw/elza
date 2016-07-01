package cz.tacr.elza.print.item;


import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

import java.io.File;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemFile extends AbstractItem<File> {

    private String name;
    private String fileName;
    private Integer fileSize;
    private String mimeType;
    private Integer pagesCount;

    public ItemFile(ArrItem arrItem, Output output, Node node, File value) {
        super(arrItem, output, node);
        setValue(value);
    }


    @Override
    public String serializeValue() {
        return getName() + " (" + getFileName() + ")";
    }
    
    public File getFile() {
        return getValue();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(Integer pagesCount) {
        this.pagesCount = pagesCount;
    }
}
