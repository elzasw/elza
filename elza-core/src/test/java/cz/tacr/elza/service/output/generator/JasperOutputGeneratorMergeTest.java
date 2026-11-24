package cz.tacr.elza.service.output.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.tacr.elza.common.CloseablePathResource;
import cz.tacr.elza.print.AttPagePlaceHolder;
import cz.tacr.elza.service.output.generator.PdfAttProvider.Attachments;
import cz.tacr.elza.service.output.generator.PdfAttProvider.PdfFileInfo;

/**
 * Sloučení PDF vygenerovaného Jasperem s PDF přílohami - odkazy na přílohy (#10254)
 */
class JasperOutputGeneratorMergeTest {

    private static final String ITEM_TYPE = "ZP2015_ATTACHMENT";

    /** Rozměr stránek přílohy - záměrně jiný než A4 Jasper stránek */
    private static final PDRectangle ATT_PAGE_SIZE = PDRectangle.A5;

    @TempDir
    Path tempDir;

    /**
     * Vstupní dokument:
     * 0 titulní list, 1 obsah, 2+3 zástupné stránky přílohy, 4 rejstřík.
     * Výstup: 0 titulní list, 1 obsah, 2+3 stránky přílohy, 4 rejstřík.
     */
    @Test
    void linksAndBookmarksToPlaceholdersPointToAttachmentPages() throws IOException {
        AttPagePlaceHolder placeHolder0 = new AttPagePlaceHolder(ITEM_TYPE, "priloha 1b", 0);
        AttPagePlaceHolder placeHolder1 = new AttPagePlaceHolder(ITEM_TYPE, "priloha 1b", 1);

        try (PDDocument inDoc = new PDDocument();
                PDDocument outDoc = new PDDocument();
                Attachments attachments = createAttachments("priloha.pdf", 2, placeHolder0, placeHolder1)) {

            PDPage title = addPage(inDoc, "Titulni list");
            PDPage toc = addPage(inDoc, "Obsah");
            PDPage placeholderPage0 = addPage(inDoc, placeHolder0.getAttPage());
            PDPage placeholderPage1 = addPage(inDoc, placeHolder1.getAttPage());
            PDPage registry = addPage(inDoc, "Rejstrik");

            // odkazy z obsahu: pevný cíl (jako Jasper), akce GoTo, odkaz na běžnou stránku
            PDAnnotationLink linkDest = addLinkWithDest(toc, xyz(placeholderPage0, 470, 822));
            PDAnnotationLink linkAction = addLinkWithAction(toc, xyz(placeholderPage1, 0, 842));
            PDPageXYZDestination registryDest = xyz(registry, 70, 786);
            PDAnnotationLink linkRegistry = addLinkWithDest(toc, registryDest);

            // záložky
            PDDocumentOutline outline = new PDDocumentOutline();
            inDoc.getDocumentCatalog().setDocumentOutline(outline);
            outline.addLast(bookmark("Příloha priloha 1b", xyz(placeholderPage0, 470, 822)));
            outline.addLast(bookmark("Rejstřík", xyz(registry, 70, 786)));

            JasperOutputGenerator.mergeDocuments(inDoc, outDoc, new ArrayList<>(List.of(attachments)));

            // struktura výstupu
            assertEquals(5, outDoc.getNumberOfPages());
            assertEquals(0, outDoc.getPages().indexOf(title));
            assertEquals(1, outDoc.getPages().indexOf(toc));
            assertEquals(4, outDoc.getPages().indexOf(registry));
            assertEquals(-1, outDoc.getPages().indexOf(placeholderPage0));
            assertEquals(-1, outDoc.getPages().indexOf(placeholderPage1));

            // odkazy
            assertTargetsAttachmentPage(outDoc, linkDest.getDestination(), 2);
            assertTargetsAttachmentPage(outDoc, ((PDActionGoTo) linkAction.getAction()).getDestination(), 3);
            assertSame(registryDest.getCOSObject(), linkRegistry.getDestination().getCOSObject(),
                    "link to regular page must stay untouched");

            // záložky
            PDOutlineItem attBookmark = outDoc.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertTargetsAttachmentPage(outDoc, attBookmark.getDestination(), 2);
            PDOutlineItem registryBookmark = attBookmark.getNextSibling();
            assertEquals(4, pageIndex(outDoc, registryBookmark.getDestination()));

            // po uložení a načtení musí odkazy mířit na stránky dokumentu
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outDoc.save(baos);
            try (PDDocument savedDoc = Loader.loadPDF(baos.toByteArray())) {
                List<PDAnnotation> links = savedDoc.getPage(1).getAnnotations();
                assertEquals(3, links.size());
                assertEquals(2, pageIndex(savedDoc, ((PDAnnotationLink) links.get(0)).getDestination()));
                assertEquals(3, pageIndex(savedDoc,
                        ((PDActionGoTo) ((PDAnnotationLink) links.get(1)).getAction()).getDestination()));
                assertEquals(4, pageIndex(savedDoc, ((PDAnnotationLink) links.get(2)).getDestination()));
                assertEquals(2, pageIndex(savedDoc,
                        savedDoc.getDocumentCatalog().getDocumentOutline().getFirstChild().getDestination()));
            }
        }
    }

    private static void assertTargetsAttachmentPage(PDDocument outDoc, PDDestination dest, int expectedIndex) {
        assertEquals(expectedIndex, pageIndex(outDoc, dest));
        PDPageXYZDestination xyz = assertInstanceOf(PDPageXYZDestination.class, dest);
        // horní okraj stránky přílohy (ne souřadnice z A4 zástupné stránky)
        assertEquals((int) Math.ceil(ATT_PAGE_SIZE.getUpperRightY()), xyz.getTop());
    }

    private static int pageIndex(PDDocument doc, PDDestination dest) {
        PDPageDestination pageDest = assertInstanceOf(PDPageDestination.class, dest);
        return doc.getPages().indexOf(pageDest.getPage());
    }

    private Attachments createAttachments(String fileName, int pageCnt, AttPagePlaceHolder... placeHolders)
            throws IOException {
        Path file = tempDir.resolve(fileName);
        try (PDDocument attDoc = new PDDocument()) {
            for (int i = 0; i < pageCnt; i++) {
                PDPage page = new PDPage(ATT_PAGE_SIZE);
                attDoc.addPage(page);
                writeText(attDoc, page, "Priloha strana " + (i + 1));
            }
            attDoc.save(file.toFile());
        }
        PdfFileInfo fileInfo = new PdfFileInfo(null, new CloseablePathResource(file), "priloha 1b");
        fileInfo.init();

        Attachments attachments = new Attachments();
        attachments.fileInfos.add(fileInfo);
        attachments.pagePlaceHolders.addAll(List.of(placeHolders));
        return attachments;
    }

    private static PDPage addPage(PDDocument doc, String text) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        writeText(doc, page, text);
        return page;
    }

    private static void writeText(PDDocument doc, PDPage page, String text) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            cs.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
            cs.showText(text);
            cs.endText();
        }
    }

    private static PDPageXYZDestination xyz(PDPage page, int left, int top) {
        PDPageXYZDestination dest = new PDPageXYZDestination();
        dest.setPage(page);
        dest.setLeft(left);
        dest.setTop(top);
        return dest;
    }

    private static PDAnnotationLink addLinkWithDest(PDPage page, PDDestination dest) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(new PDRectangle(50, 700, 200, 20));
        link.setDestination(dest);
        page.getAnnotations().add(link);
        return link;
    }

    private static PDAnnotationLink addLinkWithAction(PDPage page, PDDestination dest) throws IOException {
        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(dest);
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(new PDRectangle(50, 650, 200, 20));
        link.setAction(action);
        page.getAnnotations().add(link);
        return link;
    }

    private static PDOutlineItem bookmark(String title, PDDestination dest) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(dest);
        return item;
    }
}
