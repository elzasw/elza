package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.interpi.ws.wo.EntitaTyp;

/**
 * Typy hodnot v {@link EntitaTyp}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public enum EntityValueType {

    TRIDA("trida"),
    PODTRIDA("podtrida"),
    IDENTIFIKACE("identifikace"),
    ZAZNAM("zaznam"),
    PREFEROVANE_OZNACENI("preferovane_oznaceni"),
    VARIANTNI_OZNACENI("variantni_oznaceni"),
    UDALOST("udalost"),
    POCATEK_EXISTENCE("pocatek_existence"),
    KONEC_EXISTENCE("konec_existence"),
    ZMENA("zmena"),
    POPIS("popis"),
    SOURADNICE("souradnice"),
    TITUL("titul"),
    KODOVANE_UDAJE("kodovane_udaje"),
    SOUVISEJICI_ENTITA("souvisejici_entita"),
    HIERARCHICKA_STRUKTURA("hierarchicka_struktura"),
    ZARAZENI("zarazeni"),
    VYOBRAZENI("vyobrazeni"),
    ZDROJ_INFORMACI("zdroj_informaci");

    private String type;

    private EntityValueType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
