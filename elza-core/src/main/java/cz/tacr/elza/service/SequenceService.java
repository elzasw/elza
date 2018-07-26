package cz.tacr.elza.service;

/**
 * Rozhraní sekvenceru pro získávání identifikátorů.
 */
public interface SequenceService {

    /**
     * @param sequenceName název sekvence
     * @return další hodnota pro požadovanou sekvenci
     */
    int getNext(String sequenceName);

    /**
     * @param sequenceName název sekvence
     * @param count        požadovaný počet
     * @return další hodnoty pro požadovanou sekvenci
     */
    int[] getNext(String sequenceName, int count);

}
