package cz.tacr.elza.interpi.service.vo;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.Assert;

import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikaceTyp;
import cz.tacr.elza.interpi.ws.wo.KodovaneTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp;
import cz.tacr.elza.interpi.ws.wo.PodtridaTyp;
import cz.tacr.elza.interpi.ws.wo.PopisTyp;
import cz.tacr.elza.interpi.ws.wo.PravidlaTyp;
import cz.tacr.elza.interpi.ws.wo.SouradniceTyp;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciTyp;
import cz.tacr.elza.interpi.ws.wo.StrukturaTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TridaTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTyp;
import cz.tacr.elza.interpi.ws.wo.VyobrazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZarazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZaznamTyp;
import cz.tacr.elza.interpi.ws.wo.ZdrojTyp;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 1. 2017
 */
public class InterpiEntity {

    private Map<EntityValueType, List<Object>> valueMap;

    public InterpiEntity(final EntitaTyp entitaTyp) {
        Assert.notNull(entitaTyp);
        Assert.notNull(entitaTyp.getContent());

        convertToMap(entitaTyp);
    }

    /**
     * Převod entity do mapy, kde klíče jsou typy hodnot.
     *
     * @param entitaTyp INTERPI entita
     *
     * @return mapa hodnot, typ hodnoty -> seznam hodnot
     */
    private void convertToMap(final EntitaTyp entitaTyp) {
        Assert.notNull(entitaTyp);
        Assert.notNull(entitaTyp.getContent());

        valueMap = new HashMap<>();
        for (Serializable ser : entitaTyp.getContent()) {
            JAXBElement<?> element = (JAXBElement<?>) ser;
            addToMap(element, valueMap);
        }
    }

    private void addToMap(final JAXBElement<?> element, final Map<EntityValueType, List<Object>> result) {
        Object value = element.getValue();
        String localPart = element.getName().getLocalPart();
        switch (localPart) {
            case "trida":
                TridaTyp tridaTyp = getEntity(value, TridaTyp.class);
                putToMap(result, EntityValueType.TRIDA, tridaTyp);
                break;
            case "podtrida":
                PodtridaTyp podtridaTyp = getEntity(value, PodtridaTyp.class);
                putToMap(result, EntityValueType.PODTRIDA, podtridaTyp);
                break;
            case "identifikace":
                IdentifikaceTyp identifikaceTyp = getEntity(value, IdentifikaceTyp.class);
                putToMap(result, EntityValueType.IDENTIFIKACE, identifikaceTyp);
                break;
            case "zaznam":
                ZaznamTyp zaznamTyp = getEntity(value, ZaznamTyp.class);
                putToMap(result, EntityValueType.ZAZNAM, zaznamTyp);
                break;
            case "preferovane_oznaceni":
                OznaceniTyp prefereovane = getEntity(value, OznaceniTyp.class);
                putToMap(result, EntityValueType.PREFEROVANE_OZNACENI, prefereovane);
                break;
            case "variantni_oznaceni":
                OznaceniTyp variantni = getEntity(value, OznaceniTyp.class);
                putToMap(result, EntityValueType.VARIANTNI_OZNACENI, variantni);
                break;
            case "udalost":
                UdalostTyp udalostTyp = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.UDALOST, udalostTyp);
                break;
            case "pocatek_existence":
                UdalostTyp pocatek = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.POCATEK_EXISTENCE, pocatek);
                break;
            case "konec_existence":
                UdalostTyp konec = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.KONEC_EXISTENCE, konec);
                break;
            case "zmena":
                UdalostTyp zmena = getEntity(value, UdalostTyp.class);
                putToMap(result, EntityValueType.ZMENA, zmena);
                break;
            case "popis":
                PopisTyp popisTyp = getEntity(value, PopisTyp.class);
                putToMap(result, EntityValueType.POPIS, popisTyp);
                break;
            case "souradnice":
                SouradniceTyp souradniceTyp = getEntity(value, SouradniceTyp.class);
                putToMap(result, EntityValueType.SOURADNICE, souradniceTyp);
                break;
            case "titul":
                TitulTyp titulTyp = getEntity(value, TitulTyp.class);
                putToMap(result, EntityValueType.TITUL, titulTyp);
                break;
            case "kodovane_udaje":
                KodovaneTyp kodovaneTyp = getEntity(value, KodovaneTyp.class);
                putToMap(result, EntityValueType.KODOVANE_UDAJE, kodovaneTyp);
                break;
            case "souvisejici_entita":
                SouvisejiciTyp souvisejiciTyp = getEntity(value, SouvisejiciTyp.class);
                putToMap(result, EntityValueType.SOUVISEJICI_ENTITA, souvisejiciTyp);
                break;
            case "hierarchicka_struktura":
                StrukturaTyp strukturaTyp = getEntity(value, StrukturaTyp.class);
                putToMap(result, EntityValueType.HIERARCHICKA_STRUKTURA, strukturaTyp);
                break;
            case "zarazeni":
                ZarazeniTyp zarazeniTyp = getEntity(value, ZarazeniTyp.class);
                putToMap(result, EntityValueType.ZARAZENI, zarazeniTyp);
                break;
            case "vyobrazeni":
                VyobrazeniTyp vyobrazeniTyp = getEntity(value, VyobrazeniTyp.class);
                putToMap(result, EntityValueType.VYOBRAZENI, vyobrazeniTyp);
                break;
            case "zdroj_informaci":
                ZdrojTyp zdrojTyp = getEntity(value, ZdrojTyp.class);
                putToMap(result, EntityValueType.ZDROJ_INFORMACI, zdrojTyp);
                break;
        }
    }

    private void putToMap(final Map<EntityValueType, List<Object>> result, final EntityValueType type, final Object value) {
        List<Object> values = result.get(type);
        if (values == null) {
            values = new LinkedList<>();
            result.put(type, values);
        }
        values.add(value);
    }

    private <T> T getEntity(final Object value, final Class<T> cls) {
        return cls.cast(value);
    }

    public List<PopisTyp> getPopisTyp() {
        return getValueList(EntityValueType.POPIS);
    }

    public List<ZdrojTyp> getZdrojTyp() {
        return getValueList(EntityValueType.ZDROJ_INFORMACI);
    }

    public List<TitulTyp> getTitul() {
        return getValueList(EntityValueType.TITUL);
    }

    public List<IdentifikaceTyp> getIdentifikace() {
        return getValueList(EntityValueType.IDENTIFIKACE);
    }

    public List<OznaceniTyp> getVariantniOznaceni() {
        return getValueList(EntityValueType.VARIANTNI_OZNACENI);
    }

    public List<UdalostTyp> getPocatekExistence() {
        return getValueList(EntityValueType.POCATEK_EXISTENCE);
    }

    public List<UdalostTyp> getKonecExistence() {
        return getValueList(EntityValueType.KONEC_EXISTENCE);
    }

    public List<UdalostTyp> getUdalost() {
        return getValueList(EntityValueType.UDALOST);
    }

    public List<UdalostTyp> getZmena() {
        return getValueList(EntityValueType.ZMENA);
    }

    public List<SouvisejiciTyp> getSouvisejiciEntita() {
        return getValueList(EntityValueType.SOUVISEJICI_ENTITA);
    }

    public List<StrukturaTyp> getHierarchickaStruktura() {
        return getValueList(EntityValueType.HIERARCHICKA_STRUKTURA);
    }

    public List<KodovaneTyp> getKodovaneUdaje() {
        return getValueList(EntityValueType.KODOVANE_UDAJE);
    }

    public OznaceniTyp getPreferovaneOznaceni() {
        List<OznaceniTyp> preferovaneOznaceniList = getValueList(EntityValueType.PREFEROVANE_OZNACENI);

        // chceme typ ZP, pak INTERPI a pak je to jedno
        OznaceniTyp zp = null;
        OznaceniTyp interpi = null;
        OznaceniTyp other = null;
        for (OznaceniTyp oznaceniTyp : preferovaneOznaceniList) {
            for (PravidlaTyp pravidlaTyp : oznaceniTyp.getPravidla()) {
                if (pravidlaTyp == null) {
                    if (other == null) {
                        other = oznaceniTyp;
                    }
                } else {
                    switch (pravidlaTyp) {
                        case INTERPI:
                            if (interpi == null) {
                                interpi = oznaceniTyp;
                            }
                            break;
                        case ZP:
                            if (zp == null) {
                                zp = oznaceniTyp;
                            }
                            break;
                        default:
                            if (other == null) {
                                other = oznaceniTyp;
                            }
                    }
                }
            }
        }

        // alespoň jedno bude podle xsd vyplněné
        OznaceniTyp preferovaneOznaceni;
        if (zp != null) {
            preferovaneOznaceni = zp;
        } else if (interpi != null) {
            preferovaneOznaceni = interpi;
        } else {
            preferovaneOznaceni = other;
        }

        return preferovaneOznaceni;
    }

    public PodtridaTyp getPodTrida() {
        return getValue(EntityValueType.PODTRIDA);
    }

    public TridaTyp getTrida() {
        return getValue(EntityValueType.TRIDA);
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(final EntityValueType entityValueType) {
        List<Object> valuesList = valueMap.get(entityValueType);

        if (CollectionUtils.isEmpty(valuesList)) {
            return null;
        }
        return (T) valuesList.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getValueList(final EntityValueType entityValueType) {
        List<Object> valuesList = valueMap.get(entityValueType);

        if (CollectionUtils.isEmpty(valuesList)) {
            return Collections.emptyList();
        }

        return valuesList.stream().
                map(v -> (T) v).
                collect(Collectors.toList());
    }
}
