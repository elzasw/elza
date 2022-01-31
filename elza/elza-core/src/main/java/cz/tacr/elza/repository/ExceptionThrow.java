package cz.tacr.elza.repository;

import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;

import java.util.function.Supplier;

public class ExceptionThrow {

    public static Supplier<ObjectNotFoundException> level(final Integer levelId) {
        return () -> new ObjectNotFoundException("Nebyl nalezen level: " + levelId, BaseCode.ID_NOT_EXIST).setId(levelId);
    }

    public static Supplier<ObjectNotFoundException> version(final Integer fundVersionId) {
        return () -> new ObjectNotFoundException("Nebyla nalezena verze: " + fundVersionId, BaseCode.ID_NOT_EXIST).setId(fundVersionId);
    }

    public static Supplier<ObjectNotFoundException> fund(final Integer fundId) {
        return () -> new ObjectNotFoundException("Nebyl nalezen archivní soubor: " + fundId, BaseCode.ID_NOT_EXIST).setId(fundId);
    }

    public static Supplier<ObjectNotFoundException> template(final Integer templateId) {
        return () -> new ObjectNotFoundException("Nebyla nalezena šablona: " + templateId, BaseCode.ID_NOT_EXIST).setId(templateId);
    }

    public static Supplier<ObjectNotFoundException> refTemplate(final Integer templateId) {
        return () -> new ObjectNotFoundException("Nebyla nalezena šablona: " + templateId, BaseCode.ID_NOT_EXIST).setId(templateId);
    }

    public static Supplier<ObjectNotFoundException> refTemplateMapType(final Integer templateMapTypeId) {
        return () -> new ObjectNotFoundException("Nebylo nalezeno mapování pro šablonu: " + templateMapTypeId, BaseCode.ID_NOT_EXIST).setId(templateMapTypeId);
    }

    public static Supplier<ObjectNotFoundException> calendarType(final Integer calendarTypeId) {
        return () -> new ObjectNotFoundException("Neexistující typ kalendáře: " + calendarTypeId, BaseCode.ID_NOT_EXIST).setId(calendarTypeId);
    }

    public static Supplier<ObjectNotFoundException> outputType(final Integer outputTypeId) {
        return () -> new ObjectNotFoundException("Neexistující typ výstupu: " + outputTypeId, BaseCode.ID_NOT_EXIST).setId(outputTypeId);
    }

    public static Supplier<ObjectNotFoundException> ap(final Integer accessPointId) {
        return () -> new ObjectNotFoundException("Neexistující přístupový bod: " + accessPointId, BaseCode.ID_NOT_EXIST).setId(accessPointId);
    }

    public static Supplier<ObjectNotFoundException> scope(final Integer scopeId) {
        return () -> new ObjectNotFoundException("Nebyla nalezena oblast: " + scopeId, BaseCode.ID_NOT_EXIST).setId(scopeId);
    }

    public static Supplier<ObjectNotFoundException> output(final Integer outputId) {
        return () -> new ObjectNotFoundException("Nebyl nalezen výstup: " + outputId, BaseCode.ID_NOT_EXIST).setId(outputId);
    }

    public static Supplier<ObjectNotFoundException> node(final Integer nodeId) {
        return () -> new ObjectNotFoundException("Nebyla nalezena JP: " + nodeId, BaseCode.ID_NOT_EXIST).setId(nodeId);
    }

    public static Supplier<ObjectNotFoundException> itemType(final Integer itemTypeId) {
        return () -> new ObjectNotFoundException("Neexistuje typ: " + itemTypeId, BaseCode.ID_NOT_EXIST).setId(itemTypeId);
    }

    public static Supplier<ObjectNotFoundException> itemSpec(final Integer itemSpecId) {
        return () -> new ObjectNotFoundException("Neexistuje specifikace: " + itemSpecId, BaseCode.ID_NOT_EXIST).setId(itemSpecId);
    }

    public static Supplier<ObjectNotFoundException> institution(final Integer institutionId) {
        return () -> new ObjectNotFoundException("Neexistuje instituce: " + institutionId, BaseCode.ID_NOT_EXIST).setId(institutionId);
    }

    public static Supplier<ObjectNotFoundException> bulkAction(final Integer bulkActionId) {
        return () -> new ObjectNotFoundException("Hromadná akce nebyla nalezena: " + bulkActionId, BaseCode.ID_NOT_EXIST).setId(bulkActionId);
    }

    public static Supplier<ObjectNotFoundException> part(final Integer partId) {
        return () -> new ObjectNotFoundException("Part nenalezen: " + partId, BaseCode.ID_NOT_EXIST).setId(partId);
    }

    public static Supplier<ObjectNotFoundException> descItem(final Integer descItemId) {
        return () -> new ObjectNotFoundException("Nenalezena hodnota atributu: " + descItemId, BaseCode.ID_NOT_EXIST).setId(descItemId);
    }

    public static Supplier<ObjectNotFoundException> ruleSet(final Integer ruleSetId) {
        return () -> new ObjectNotFoundException("Nenalezena pravidla: " + ruleSetId, BaseCode.ID_NOT_EXIST).setId(ruleSetId);
    }

    public static Supplier<ObjectNotFoundException> issueState(final Integer issueStateId) {
        return () -> new ObjectNotFoundException("Stav protokolu nenalezen: " + issueStateId, BaseCode.ID_NOT_EXIST).setId(issueStateId);
    }

    public static Supplier<ObjectNotFoundException> issueType(final Integer issueTypeId) {
        return () -> new ObjectNotFoundException("Typ protokolu nenalezen: " + issueTypeId, BaseCode.ID_NOT_EXIST).setId(issueTypeId);
    }

    public static Supplier<ObjectNotFoundException> issueList(final Integer issueListId) {
        return () -> new ObjectNotFoundException("Protokol nenalezen: " + issueListId, BaseCode.ID_NOT_EXIST).setId(issueListId);
    }

    public static Supplier<ObjectNotFoundException> issue(final Integer issueId) {
        return () -> new ObjectNotFoundException("Připomínka nenalezena: " + issueId, BaseCode.ID_NOT_EXIST).setId(issueId);
    }

    public static Supplier<ObjectNotFoundException> comment(final Integer commentId) {
        return () -> new ObjectNotFoundException("Komentář nenalezen: " + commentId, BaseCode.ID_NOT_EXIST).setId(commentId);
    }

    public static Supplier<ObjectNotFoundException> file(final Integer fileId) {
        return () -> new ObjectNotFoundException("Soubor nenalezen: " + fileId, BaseCode.ID_NOT_EXIST).setId(fileId);
    }

    public static Supplier<ObjectNotFoundException> structureData(final Integer structureDataId) {
        return () -> new ObjectNotFoundException("Strukturovaný objekt nenalezen: " + structureDataId, BaseCode.ID_NOT_EXIST).setId(structureDataId);
    }

    public static Supplier<ObjectNotFoundException> outputResult(final Integer outputResultId) {
        return () -> new ObjectNotFoundException("Výsledek výstupu nenalezen: " + outputResultId, BaseCode.ID_NOT_EXIST).setId(outputResultId);
    }

    public static Supplier<ObjectNotFoundException> partType(final Integer partTypeId) {
        return () -> new ObjectNotFoundException("Nenalezen typ partu: " + partTypeId, BaseCode.ID_NOT_EXIST).setId(partTypeId);
    }

    public static Supplier<ObjectNotFoundException> outputFilter(final Integer outputFilterId) {
        return () -> new ObjectNotFoundException("Filter výstupu nenalezen: " + outputFilterId, BaseCode.ID_NOT_EXIST).setId(outputFilterId);
    }

    public static Supplier<ObjectNotFoundException> revPart(final Integer partId) {
        return () -> new ObjectNotFoundException("Part revize nenalezen: " + partId, BaseCode.ID_NOT_EXIST).setId(partId);
    }

}
