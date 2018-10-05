package cz.tacr.elza.service.output.generator;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import cz.tacr.elza.common.CloseablePathResource;
import cz.tacr.elza.core.AppContext;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.AttPagePlaceHolder;
import cz.tacr.elza.print.AttPageProvider;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.attachment.AttachmentService;

/**
 * PDF Attachment page provider
 */
public class PdfAttProvider implements AttPageProvider {

    /**
     * File info for the PDF attachment
     * 
     */
    static class PdfFileInfo
            implements Closeable {

        int pageCnt = 0;

        /**
         * Resource with PDF file
         * 
         */
        final CloseablePathResource resource;

        final private File printFile;

        private PDDocument pdfDoc;

        public PdfFileInfo(final File printFile, final CloseablePathResource resource) {
            this.printFile = printFile;
            this.resource = resource;
        }

        public int getPageCnt() {
            return pageCnt;
        }

        public PDDocument getPdfDocument() {
            return pdfDoc;
        }

        public void init() {
            // read number of pages
            if (resource != null) {
                try {
                    java.io.File inputFile = resource.getPath().toFile();
                    pdfDoc = PDDocument
                            .load(inputFile,
                                  MemoryUsageSetting.setupMixed(JasperOutputGenerator.MAX_PDF_MAIN_MEMORY_BYTES));
                    pageCnt = pdfDoc.getNumberOfPages();
                } catch (IOException e) {
                    throw new SystemException("Failed to read number of PDF pages", e, BaseCode.SYSTEM_ERROR)
                            .set("fileId", printFile.getFileId())
                            .set("fileName", printFile.getFileName());
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (pdfDoc != null) {
                pdfDoc.close();
                pdfDoc = null;
            }
            if (resource != null) {
                resource.close();
            }

        }

    }

    /**
     * Collection of attachments for given itemType
     * 
     *
     */
    static class Attachments
            implements Closeable {
        /**
         * List of PDF attachments
         */
        List<PdfFileInfo> fileInfos = new ArrayList<>();

        List<AttPagePlaceHolder> pagePlaceHolders = new ArrayList<>();

        public List<AttPagePlaceHolder> getPagePlaceHolders() {
            return pagePlaceHolders;
        }

        public void addAttachment(Item item, PdfFileInfo fileInfo) {

            fileInfos.add(fileInfo);

            // get number of pages
            int pageCnt = fileInfo.getPageCnt();

            // prepare placeholders
            for (int i = 0; i < pageCnt; i++) {
                AttPagePlaceHolder attPlaceHolder = new AttPagePlaceHolder(item.getType().getCode());
                pagePlaceHolders.add(attPlaceHolder);
            }
        }

        @Override
        public void close() throws IOException {
            // close
            for (PdfFileInfo fi : fileInfos) {
                fi.close();
            }
            fileInfos.clear();
        }

        /**
         * add all pages into input document
         * 
         * @param inDoc
         * @return Return number of added pages
         * @throws IOException
         */
        public int addAllPages(PDDocument outDoc) throws IOException {
            int cnt = 0;
            for(PdfFileInfo fileInfo: fileInfos) {
                PDDocument mergedDoc = fileInfo.getPdfDocument();
                for (PDPage page : mergedDoc.getPages()) {
                    outDoc.addPage(page);
                }
            }
            return cnt;
        }

    }

    /**
     * Attachments by item type
     */
    Map<String, Attachments> attachmentsByType = new HashMap<>();

    private final DmsService dmsService = AppContext.getBean(DmsService.class);
    private final AttachmentService attachmentService = AppContext.getBean(AttachmentService.class);

    private Output output;

    /**
     * Create PDF file from attachment (if possible)
     * 
     * @param printFile
     * @return
     */
    private PdfFileInfo createFileInfo(File printFile) {
        final DmsFile dmsFile = dmsService.getFile(printFile.getFileId());
        CloseablePathResource resource = attachmentService.generate(dmsFile, DmsService.MIME_TYPE_APPLICATION_PDF);

        PdfFileInfo pdfFi = new PdfFileInfo(printFile, resource);
        pdfFi.init();
        return pdfFi;
    }

    @Override
    public void close() throws IOException {
        // close all file infos
        for (Attachments att : attachmentsByType.values()) {
            att.close();
        }
        attachmentsByType.clear();

        output = null;
    }

    public void setOutput(final Output outputModel) {
        this.output = outputModel;
    }

    @Override
    public List<AttPagePlaceHolder> getAttPagePlaceHolders(String itemTypeCode) {

        // check if we have already placeholders
        Attachments atts = attachmentsByType.get(itemTypeCode);
        if (atts == null) {
            atts = createAttachments(itemTypeCode);
            attachmentsByType.put(itemTypeCode, atts);
        }

        return atts.getPagePlaceHolders();
    }

    private Attachments createAttachments(String itemTypeCode) {

        Attachments atts = new Attachments();

        // find all item types
        List<Item> outputItems = output.getItems(Collections.singletonList(itemTypeCode));

        outputItems.forEach(item -> {
            // item type have to be file ref
            Validate.isTrue(item.getType().getDataType() == DataType.FILE_REF,
                            "Item type has to be file-ref, itemTypeCode: {}", itemTypeCode);
            File printFile = item.getValue(File.class);

            PdfFileInfo fileInfo = createFileInfo(printFile);

            atts.addAttachment(item, fileInfo);
        });

        return atts;
    }

    /**
     * Return total number of attached pages
     * 
     * @return
     */
    public int getTotalPageCnt() {
        int cnt = 0;
        for (Attachments att : attachmentsByType.values()) {
            cnt += att.getPagePlaceHolders().size();
        }
        return cnt;
    }

    /**
     * Return prepared attachments
     * 
     * @return
     */
    public Collection<Attachments> getAttachments() {
        return Collections.unmodifiableCollection(attachmentsByType.values());
    }

}
