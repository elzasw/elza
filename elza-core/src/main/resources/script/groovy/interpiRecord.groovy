import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.interpi.service.pqf.PQFQueryBuilder;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.wo.DoplnekTyp;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikaceTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikatorTypA;
import cz.tacr.elza.interpi.ws.wo.KodovaneTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTypTypA;
import cz.tacr.elza.interpi.ws.wo.PodtridaTyp;
import cz.tacr.elza.interpi.ws.wo.PopisTyp;
import cz.tacr.elza.interpi.ws.wo.PravidlaTyp;
import cz.tacr.elza.interpi.ws.wo.SouradniceTyp;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciTyp;
import cz.tacr.elza.interpi.ws.wo.StrukturaTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTypA;
import cz.tacr.elza.interpi.ws.wo.TridaTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTyp;
import cz.tacr.elza.interpi.ws.wo.VedlejsiCastTyp;
import cz.tacr.elza.interpi.ws.wo.VyobrazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZarazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZaznamTyp;
import cz.tacr.elza.interpi.ws.wo.ZdrojTyp;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.PartyType;

OznaceniTyp oznaceniTyp = FACTORY.getPreferovaneOznaceni(VALUE_MAP);
RECORD.setName(generatePartyNameString(oznaceniTyp, VALUE_MAP));


    /**
     * Podle jména osoby provede vygenerování textu jména.
     * @param partyName jméno osoby
     * @param partyType typ osoby
     * @return text rejstříkového hesla
     */
    String generatePartyNameString(final OznaceniTyp oznaceniTyp, final Map<EntityValueType, List<Object>> valueMap) {
        Assert.assertNotNull(oznaceniTyp);

        List<String> recordNames = new ArrayList<>();
        recordNames.add(oznaceniTyp.getHlavniCast().getValue());
        VedlejsiCastTyp vedlejsiCast = oznaceniTyp.getVedlejsiCast();
        if (vedlejsiCast != null) {
            recordNames.add(vedlejsiCast.getValue());
        }

        Set<String> degreesBefore = new HashSet<>();
        Set<String> degreesAfter = new HashSet<>();
        List<TitulTyp> titulTypList = FACTORY.getTitul(valueMap);
        for (TitulTyp titulTyp : titulTypList) {
            if (TitulTypA.TITULY_PŘED_JMÉNEM == titulTyp.getTyp()) {
                degreesBefore.add(titulTyp.getValue());
            } else if (TitulTypA.TITULY_ZA_JMÉNEM == titulTyp.getTyp()) {
                degreesAfter.add(titulTyp.getValue());
            }
        }
        recordNames.add(StringUtils.join(degreesBefore, " "));
        recordNames.add(StringUtils.join(degreesAfter, " "));

//        List<ParPartyNameComplement> sortedComplements =
//                sortNameComplements(oznaceniTyp.getPartyNameComplements(), partyType);

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

//                String parPartyNameComplementName = doplnekTyp.getTyp().value();
//                ParComplementType parComplementType = complementTypeRepository.findByName(parPartyNameComplementName);
//
//                parPartyNameComplement.setComplementType(parComplementType);

                parPartyNameComplements.add(parPartyNameComplement);
            }
        }

        return parPartyNameComplements;
    }
