import org.apache.commons.lang.StringUtils;
import org.junit.Assert
import cz.tacr.elza.domain.ParPartyNameComplement
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.wo.DoplnekTyp;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp
import cz.tacr.elza.interpi.ws.wo.PopisTyp
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTypA
import cz.tacr.elza.interpi.ws.wo.VedlejsiCastTyp
import cz.tacr.elza.utils.XmlUtils;
import cz.tacr.elza.interpi.service.vo.InterpiEntity;

List<ExternalRecordVO> records = new LinkedList<>();
for (entitaTyp in ENTITIES) {
    InterpiEntity interpiEntity = new InterpiEntity(entitaTyp);
    OznaceniTyp oznaceniTyp = interpiEntity.getPreferovaneOznaceni();

    ExternalRecordVO record = new ExternalRecordVO();
    records.add(record);

    Iterator<PopisTyp> iterator = interpiEntity.getPopisTyp().iterator();
    if (iterator.hasNext()) {
        List<String> details = new ArrayList<>();
        while(iterator.hasNext()) {
            PopisTyp popis = iterator.next();
            details.add(popis.getTextPopisu());
        }
        record.setDetail(details.join("; "));
    } else {
        record.setDetail(createSimpleDetail(entitaTyp));
    }

    record.setName(generatePartyNameString(oznaceniTyp, interpiEntity));
    record.setRecordId(FACTORY.getInterpiRecordId(interpiEntity));

    List<OznaceniTyp> otherNames = interpiEntity.getVariantniOznaceni();
    List<String> variantNames = new ArrayList<>(otherNames.size());
    otherNames.each {
        String variantRecord = createVariantRecord(it, interpiEntity)
        variantNames.add(variantRecord);
    };
    record.setVariantNames(variantNames);
}

return records;

String createSimpleDetail(EntitaTyp entitaTyp) {
    byte[] marshallData = XmlUtils.marshallData(entitaTyp, EntitaTyp.class);

    return XmlUtils.formatXml(new ByteArrayInputStream(marshallData));
}

String createDetail(InterpiEntity ent) {

    StringBuilder detail = new StringBuilder();
    detail.append("id: ").append(FACTORY.getInterpiRecordId(interpiEntity)).append("\n")
    detail.append("název: ").append(generatePartyNameString(ent)).append("\n");
    detail.append("popis: ");
    Iterator<PopisTyp> iterator = interpiEntity.getPopisTyp().iterator();
    if (iterator.hasNext()) {
        List<String> details = new ArrayList<>();
        while(iterator.hasNext()) {
            PopisTyp popis = iterator.next();
            details.add(popis.getTextPopisu());
        }
        detail.append(details.join("; "));
    }



    return XmlUtils.formatXml(new ByteArrayInputStream(marshallData));
}

/**
 * Vytvoří variantní rejstříkové heslo.
 * @param oznaceniTyp jméno osoby
 * @param interpiEntity data osoby
 * @return variantní rejstříkové heslo
 */
String createVariantRecord(final OznaceniTyp oznaceniTyp, final InterpiEntity interpiEntity) {
    return generatePartyNameString(oznaceniTyp, interpiEntity);
}

String generatePartyNameString(final InterpiEntity interpiEntity) {
    return generatePartyNameString(interpiEntity.getPreferovaneOznaceni(), interpiEntity);
}

/**
 * Podle jména osoby provede vygenerování textu jména.
 * @return text rejstříkového hesla
 */
String generatePartyNameString(final OznaceniTyp oznaceniTyp, final InterpiEntity interpiEntity) {
    Assert.assertNotNull(oznaceniTyp);

    List<String> recordNames = new ArrayList<>();
    recordNames.add(oznaceniTyp.getHlavniCast().getValue());
    VedlejsiCastTyp vedlejsiCast = oznaceniTyp.getVedlejsiCast();
    if (vedlejsiCast != null) {
        recordNames.add(vedlejsiCast.getValue());
    }

    Set<String> degreesBefore = new HashSet<>();
    Set<String> degreesAfter = new HashSet<>();
    List<TitulTyp> titulTypList = interpiEntity.getTitul();
    for (TitulTyp titulTyp : titulTypList) {
        if (TitulTypA.TITULY_PŘED_JMÉNEM == titulTyp.getTyp()) {
            degreesBefore.add(titulTyp.getValue());
        } else if (TitulTypA.TITULY_ZA_JMÉNEM == titulTyp.getTyp()) {
            degreesAfter.add(titulTyp.getValue());
        }
    }
    recordNames.add(StringUtils.join(degreesBefore, " "));
    recordNames.add(StringUtils.join(degreesAfter, " "));

//    List<ParPartyNameComplement> sortedComplements =
//            sortNameComplements(oznaceniTyp.getPartyNameComplements(), partyType);

    List<ParPartyNameComplement> partyNameComplements = createPartyNameComplements(oznaceniTyp.getDoplnek());
    partyNameComplements.each {
        recordNames.add(it.getComplement() + ",");
    }

    String recordName = StringUtils.join(recordNames, " ");
    recordName = recordName.replaceAll("\\s+", " ").trim();
    recordName = StringUtils.strip(recordName, ",");

    return recordName;
}

List<ParPartyNameComplement> createPartyNameComplements(final List<DoplnekTyp> doplnekTypList) {
    List<ParPartyNameComplement> parPartyNameComplements = new LinkedList<>();

    if (doplnekTypList != null) {
    for (DoplnekTyp doplnekTyp : doplnekTypList) {
            ParPartyNameComplement parPartyNameComplement = new ParPartyNameComplement();
            parPartyNameComplement.setComplement(doplnekTyp.getValue());

//            String parPartyNameComplementName = doplnekTyp.getTyp().value();
//            ParComplementType parComplementType = complementTypeRepository.findByName(parPartyNameComplementName);
//
//            parPartyNameComplement.setComplementType(parComplementType);

            parPartyNameComplements.add(parPartyNameComplement);
        }
    }

    return parPartyNameComplements;
}
