package cz.tacr.elza.service;

/**
 * Rozhraní sekvenceru pro získávání identifikátorů.
 */
public interface SequenceService {

    /**
     * @return další hodnota pro požadovanou sekvenci
     */
    int getNext(String sequenceName);

}
