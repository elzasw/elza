package script.groovy

import cz.tacr.elza.domain.*
import cz.tacr.elza.domain.convertor.UnitDateConvertor
import cz.tacr.elza.service.party.ApConvName
import cz.tacr.elza.service.party.ApConvResult
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.Validate

/**
 * Skript pro vytvoření rejstříkového hesla z osoby.
 */

final ApConvResult convResult = new ApConvResult();

checkApState(AP_STATE);
checkParty(PARTY);

String desc = generateCharacteristics(PARTY);
convResult.setDescription(desc);
prepareNames(PARTY, convResult);

return convResult;


void prepareNames(ParParty party, ApConvResult convResult) {
	ParPartyType partyType = party.getPartyType();
	// prepare preferred name
	ParPartyName prefName = party.getPreferredName();
	ApConvName convPrefName = createConvName(prefName, partyType);
	convResult.addName(convPrefName);
	// prepare other names
	for (name in party.getPartyNames()) {
        if (name.getPartyNameId().equals(prefName.getPartyNameId())) {
			continue; // skip preferred name
		}
		ApConvName convName = createConvName(name, partyType);
		convResult.addName(convName);
	}
}

/**
 * Vytvoří rejstříkové heslo.
 * @param partyName jméno osoby
 * @param partyType typ osoby
 * @return rejstříkové heslo
 */
ApConvName createConvName(final ParPartyName partyName, final ParPartyType partyType) {
    String name = generatePartyNameString(partyName, partyType);

    ApConvName convName = new ApConvName();
    convName.setName(name);
    return convName;
}

/**
 * Podle jména osoby provede vygenerování textu jména.
 * @param partyName jméno osoby
 * @param partyType typ osoby
 * @return text rejstříkového hesla
 */
String generatePartyNameString(final ParPartyName partyName, final ParPartyType partyType) {
    Validate.notNull(partyName);

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
List<ParPartyNameComplement> sortNameComplements(final List<ParPartyNameComplement> complements,
                                                 final ParPartyType parPartyType) {
    if (CollectionUtils.isEmpty(complements)) {
        return Collections.EMPTY_LIST;
    }
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
        final String VZNIK = ParRelationClassType.CREATION_CODE;
        for (ParRelation relation : party.getRelations()) {
            if (relation.getRelationType().getRelationClassType().getCode().equals(VZNIK)
                    && relation.getFrom() != null) {
                fromDate = relation.getFrom();
                break;
            }
        }
    }

    //konec existence
    ParUnitdate toDate = null;
    if (party.getRelations() != null) {
        final String ZANIK = ParRelationClassType.DESTRUCTION_CODE;
        for (ParRelation relation : party.getRelations()) {
            if (relation.getRelationType().getRelationClassType().getCode().equals(ZANIK)
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
    Validate.notNull(party);
    if (party.getPreferredName() == null) {
        throw new IllegalArgumentException("Osoba nemá nastaveno preferované jméno.");
    }
}

/**
 * Kontrola zadaných dat přístupového bodu.
 * @param apState přístupový bod
 */
void checkApState(ApState apState) {
    Validate.notNull(apState);
    Validate.notNull(apState.getAccessPoint());
    if (apState.getScope() == null) {
        throw new IllegalArgumentException("Není nastavena třída rejstříku.");
    }
}
