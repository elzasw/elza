package cz.tacr.elza.domain.interfaces;

/**
 * Rozhraní pro společnou textovou hodnotu hodnoty atributu.
 * Např. String, Text a Formatted text mají reálně stejný datový typ - String, takže je lze sjednoceně porovnávat.
 *
 * @author Martin Šlapa
 * @since 27.10.2016
 */
@Deprecated
public interface IArrItemStringValue {

    /**
     * @return hodnota atributu
     */
    String getValue();

    /**
     * @param value hodnota atributu
     */
    void setValue(String value);

}
