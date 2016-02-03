package cz.tacr.elza.repository;

import java.io.Serializable;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;


/**
 * Rozšíření JpaRepository o vlastní metody.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
@NoRepositoryBean
public interface ElzaJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    /**
     * Najde entitu podle id. Pokud není entita nalezena, vyhodí výjimku pokud není objekt nalezen.
     *
     * @param id id objektu
     * @return nalezená instance objektu
     * @throws IllegalStateException objekt nebyl nalezen
     */
    default T getOneCheckExist(ID id) throws IllegalStateException {
        T result = getOne(id);


        if (result == null) {
            throw new IllegalStateException("Nebyla nalezena entita v úložišti " + getClassName() + " s id " + id);
        } else {
            try {
                if (result instanceof HibernateProxy) {
                    HibernateProxy proxy = (HibernateProxy) result;

                    //provedeme inicializaci, která nám zaručí existenci entity
                    proxy.getHibernateLazyInitializer().initialize();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Nebyla nalezena entita " + getClassName() + " s id " + id);
            }
        }

        return result;
    }


    /**
     * Vrací název DO objektu.
     *
     * @return název DO objektu
     */
    String getClassName();
}
