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
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.common.CloseablePathResource;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.AttPagePlaceHolder;
import cz.tacr.elza.print.AttPageProvider;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFileRef;
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

        final String attachmentName;

        private PDDocument pdfDoc;

        public PdfFileInfo(final File printFile, final CloseablePathResource resource, final String attachmentName) {
            this.printFile = printFile;
            this.resource = resource;
            this.attachmentName = attachmentName;
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

        public String getName() {
            return attachmentName;
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
            String attachmentName = fileInfo.getName();

            // prepare placeholders
            for (int i = 0; i < pageCnt; i++) {
                AttPagePlaceHolder attPlaceHolder = new AttPagePlaceHolder(item.getType().getCode(), attachmentName, i);
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

    private final DmsService dmsService;
    private final AttachmentService attachmentService;

    private Output output;

    public PdfAttProvider(ApplicationContext applicationContext) {
        dmsService = applicationContext.getBean(DmsService.class);
        attachmentService = applicationContext.getBean(AttachmentService.class);
    }

    /**
     * Create PDF file from attachment (if possible)
     * 
     * @param printFile
     * @param name
     *            Name of attachment
     * @return
     */
    private PdfFileInfo createFileInfo(File printFile, String name) {
        final DmsFile dmsFile = dmsService.getFile(printFile.getFileId());
        CloseablePathResource resource = attachmentService.generate(dmsFile, DmsService.MIME_TYPE_APPLICATION_PDF);

        PdfFileInfo pdfFi = new PdfFileInfo(printFile, resource, name);
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

            ItemFileRef fileRef = (ItemFileRef) item;
            String attachmentName = fileRef.getName();

            File printFile = item.getValue(File.class);

            PdfFileInfo fileInfo = createFileInfo(printFile, attachmentName);

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
