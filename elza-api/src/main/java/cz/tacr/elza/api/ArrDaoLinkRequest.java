package cz.tacr.elza.api;

/**
 * Dotaz pro externí systémy - připojení / odpojení DAO k JP.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrDaoLinkRequest<DR extends ArrDigitalRepository, N extends ArrNode, D extends ArrDao> {

    DR getDigitalRepository();

    void setDigitalRepository(DR digitalRepository);

    D getDao();

    void setDao(D dao);

    N getNode();

    void setNode(N node);

    Type getType();

    void setType(Type type);

    enum Type {

        /**
         * Připojení k JP.
         */
        LINK,

        /**
         * Odpojení od JP.
         */
        UNLINK

    }

}
