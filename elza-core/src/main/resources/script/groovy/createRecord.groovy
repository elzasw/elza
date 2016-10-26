import cz.tacr.elza.domain.*
import cz.tacr.elza.domain.convertor.UnitDateConvertor
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.springframework.util.Assert

import javax.annotation.Nullable


/**
 * Skript pro vytvoření rejstříkového hesla z osoby.
 */


ParParty party = PARTY;
return createRecord(party);


/**
 * Provede vytvoření rejstříkového hesla podle dat osoby.
 * @param party data osoby
 * @return vytvořené rejstříkové heslo
 */
RegRecord createRecord(ParParty party) {
    Assert.notNull(party);
    checkParty(party);

    ParPartyName preferedName = party.getPreferredName();

    List<ParPartyName> otherNames = new ArrayList<>();
    if (party.getPartyNames() != null) {
        party.getPartyNames().each {
            if (!it.is(preferedName)) {
                otherNames.add(it);
            }
        }
    }


    RegRecord record = new RegRecord();
    record.setNote(party.getRecord().getNote());
    record.setRegisterType(party.getRecord().getRegisterType());
    record.setScope(party.getRecord().getScope());
    record.setRecord(generatePartyNameString(preferedName, party.getPartyType()));
    record.setCharacteristics(generateCharacteristics(party));

    List<RegVariantRecord> variantRecords = new ArrayList<>(otherNames.size());
    otherNames.each {
        RegVariantRecord variantRecord = createVariantRecord(it, party.getPartyType())
        variantRecords.add(variantRecord);
    };
    record.setVariantRecordList(variantRecords);

    return record;
}


/**
 * Podle jména osoby provede vygenerování textu jména.
 * @param partyName jméno osoby
 * @param partyType typ osoby
 * @return text rejstříkového hesla
 */
String generatePartyNameString(final ParPartyName partyName, final ParPartyType partyType) {
    Assert.notNull(partyName);

    List<ParPartyNameComplement> sortedComplements =
            sortNameComplements(partyName.getPartyNameComplements(), partyType);


    List<String> recordNames = new ArrayList<>();
    recordNames.add(partyName.getMainPart());
    recordNames.add(partyName.getOtherPart());
    recordNames.add(partyName.getDegreeBefore());
    recordNames.add(partyName.getDegreeAfter());

    sortedComplements.each {
        recordNames.add(it.getComplement() + ",");
    }

    String recordName = StringUtils.join(recordNames, " ");
    recordName = recordName.replaceAll("\\s+", " ").trim();
    recordName = StringUtils.strip(recordName, ",");

    return recordName;
}



/**
 * Provede seřazení doplňků jmen podle typu.
 * @param complements seznam doplňků jména
 * @param parPartyType typ osoby
 * @return seřazený seznam doplňků jména
 */
List<ParPartyNameComplement> sortNameComplements(@Nullable final List<ParPartyNameComplement> complements,
                                                 final ParPartyType parPartyType) {
    if (CollectionUtils.isEmpty(complements)) {
        return Collections.EMPTY_LIST;
    } else {
        final Map<Integer, ParComplementType> complementTypeMap = COMPLEMENT_TYPE_MAP;

        return complements.sort(false, { a, b ->
            Integer aComplementTypeId = a.getComplementType().getComplementTypeId();
            Integer bComplementTypeId = b.getComplementType().getComplementTypeId();

            if (aComplementTypeId == null || bComplementTypeId == null) {
                throw new IllegalStateException("Není nastaven typ doplňku jména.");
            }

            ParComplementType aType = complementTypeMap.get(aComplementTypeId);
            ParComplementType bType = complementTypeMap.get(bComplementTypeId);

            if (aType == null || bType == null) {
                throw new IllegalStateException(
                        "Typ doplňku jména není nastaven pro osoby typu " + parPartyType.getName());
            }

            return aType.getViewOrder().compareTo(bType.getViewOrder());
        });
    }
}


/**
 * Vytvoří variantní rejstříkové heslo.
 * @param partyName jméno osoby
 * @param partyType typ osoby
 * @return variantní rejstříkové heslo
 */
RegVariantRecord createVariantRecord(final ParPartyName partyName, final ParPartyType partyType) {
    RegVariantRecord variantRecord = new RegVariantRecord();
    variantRecord.setRecord(generatePartyNameString(partyName, partyType));
    return variantRecord;
}


/**
 * Generování charakteristiky hesla.
 * <br/>
 * [[počátek existence]-[konec existence] ][hierarchická struktura preferovaných jmen nadřazených rejstříkových hesel] [charakteristika]
 * <ul>
 * <li> počátek existence = pokud je vyplněná par_party.from_unitdate, tak par_party.from_unitdate, jinak najít libovolný první vztah třídy vznik (par_relation_type.class_type == "B"), který má vyplněnou from_unitdate a použít tuto</li>
 * <li>konec existence = pokud je vyplněná par_party.to_unitdate, tak par_party.to_unitdate, jinak najít libovolný první vztah třídy zánik (par_relation_type.class_type == "E"), který má vyplněnou to_unitdate a použít tuto</li>
 * </ul>
 * @param party osoba hesla
 * @return charakteristika hesla
 */
String generateCharacteristics(ParParty party) {

    //počátek existence
    ParUnitdate fromDate = null;
    if(party.getRelations() != null) {
        final ParRelationClassType.ClassType VZNIK = ParRelationClassType.ClassType.VZNIK;
        for (ParRelation relation : party.getRelations()) {
            if (relation.getComplementType().getRelationClassType().getCode().equals(VZNIK.getClassType())
                    && relation.getFrom() != null) {
                fromDate = relation.getFrom();
                break;
            }
        }
    }

    //konec existence
    ParUnitdate toDate = null;
    if (party.getRelations() != null) {
        final ParRelationClassType.ClassType ZANIK = ParRelationClassType.ClassType.ZANIK;
        for (ParRelation relation : party.getRelations()) {
            if (relation.getComplementType().getRelationClassType().getCode().equals(ZANIK.getClassType())
                    && relation.getTo() != null) {
                toDate = relation.getTo();
                break;
            }
        }
    }

    String fromString = fromDate == null ? "" : UnitDateConvertor.convertParUnitDateToString(fromDate);
    String toString = toDate == null ? "" : UnitDateConvertor.convertParUnitDateToString(toDate);

    StringBuilder builder = new StringBuilder();

    builder.append(fromString);
    builder.append(fromDate != null && toDate != null ? " - " : "");
    builder.append(toString + " ");
    builder.append(party.getCharacteristics() == null ? "" : party.getCharacteristics());

    return builder.toString();
}




/**
 * Kontrola zadaných dat osoby.
 * @param party osoba
 */
void checkParty(ParParty party) {

    if (party.getRecord() == null || party.getRecord().getScope() == null ||
            party.getRecord().getScope().getScopeId() == null) {
        throw new IllegalArgumentException("Není nastavena třída rejstříku.");
    }

    if (party.getPreferredName() == null) {
        throw new IllegalArgumentException("Osoba nemá nastaveno preferované jméno.");
    }

}






























