package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Přesměrování odkazů z vynechaných zástupných stránek příloh na vložené stránky příloh.
 *
 * Jasper zapisuje odkazy (např. z obsahu) jako pevný cíl s referencí na objekt stránky.
 * Zástupná stránka přílohy se při slučování do výstupního PDF nepřenáší, odkaz na ni by
 * tedy mířil na stránku mimo dokument a prohlížeč by zobrazil první stránku.
 *
 * Měněny jsou pouze cíle mířící na nahrazenou stránku, ostatní odkazy zůstávají beze změny.
 */
class ReplacedPagesRedirector {

    private static final Logger logger = LoggerFactory.getLogger(ReplacedPagesRedirector.class);

    /**
     * Nahrazená (vynechaná) stránka -> stránka, která ji ve výstupu nahradila
     */
    private final Map<COSDictionary, PDPage> replacedPages = new IdentityHashMap<>();

    /**
     * Zaznamená náhradu zástupné stránky
     *
     * @param placeholderPage
     *            vynechaná zástupná stránka
     * @param addedPages
     *            stránky vložené místo skupiny zástupných stránek
     * @param index
     *            pořadí zástupné stránky ve skupině (od 0)
     */
    void addReplacement(PDPage placeholderPage, List<PDPage> addedPages, int index) {
        if (addedPages.isEmpty()) {
            return;
        }
        // počet zástupných stránek odpovídá počtu stránek přílohy, pro jistotu omezíme index
        PDPage target = addedPages.get(Math.min(index, addedPages.size() - 1));
        replacedPages.put(placeholderPage.getCOSObject(), target);
    }

    boolean isEmpty() {
        return replacedPages.isEmpty();
    }

    /**
     * Přesměruje odkazy (link anotace) na daných stránkách
     *
     * @param pages
     *            stránky, jejichž odkazy se mají zkontrolovat
     */
    void redirectLinks(Iterable<PDPage> pages) {
        if (isEmpty()) {
            return;
        }
        for (PDPage page : pages) {
            List<PDAnnotation> annotations;
            try {
                annotations = page.getAnnotations();
            } catch (IOException e) {
                logger.warn("Failed to read page annotations, links are not redirected", e);
                continue;
            }
            for (PDAnnotation annotation : annotations) {
                if (annotation instanceof PDAnnotationLink link) {
                    redirectLink(link);
                }
            }
        }
    }

    private void redirectLink(PDAnnotationLink link) {
        try {
            PDPageDestination newDest = redirect(link.getDestination());
            if (newDest != null) {
                link.setDestination(newDest);
                return;
            }
            PDAction action = link.getAction();
            if (action instanceof PDActionGoTo goTo) {
                newDest = redirect(goTo.getDestination());
                if (newDest != null) {
                    goTo.setDestination(newDest);
                }
            }
        } catch (IOException e) {
            // odkaz ponecháme beze změny
            logger.warn("Failed to read link destination, link is not redirected", e);
        }
    }

    /**
     * Vrátí nový cíl, pokud původní cíl míří na nahrazenou stránku
     *
     * @param dest
     *            původní cíl
     * @return nový cíl nebo null, pokud cíl není potřeba měnit
     */
    PDPageDestination redirect(PDDestination dest) {
        if (isEmpty() || !(dest instanceof PDPageDestination pageDest)) {
            return null;
        }
        PDPage page = pageDest.getPage();
        if (page == null) {
            return null;
        }
        PDPage target = replacedPages.get(page.getCOSObject());
        if (target == null) {
            return null;
        }
        // stránka přílohy může mít jiný rozměr -> cílem je její horní okraj
        PDRectangle box = target.getCropBox();
        PDPageXYZDestination newDest = new PDPageXYZDestination();
        newDest.setPage(target);
        newDest.setLeft((int) box.getLowerLeftX());
        newDest.setTop((int) Math.ceil(box.getUpperRightY()));
        return newDest;
    }
}
