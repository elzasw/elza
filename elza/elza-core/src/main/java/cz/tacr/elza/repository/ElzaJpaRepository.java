package cz.tacr.elza.repository;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * Rozšíření JpaRepository o vlastní metody.
 *
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
    default T getOneCheckExist(final ID id) throws IllegalStateException {
        return findById(id)
                .orElseThrow(() -> new BusinessException("Nebyla nalezena entita v úložišti " + getClassName() + " s id " + id,
                		BaseCode.ID_NOT_EXIST)
                		.set("ID", id)
                		.set("Class", getClassName())
                		);
    }

    /**
     * Vrací název DO objektu. Pokud v některých případech nebude fungovat automatické zjištění, je nutné v konkrétním
     * repository tuto metodu překrýt.
     *
     * @return název DO objektu
     */
    default String getClassName() {
        try {
            // Načtení názvu entity z generic parametru rozhraní repository
            ParameterizedType pt = (ParameterizedType) Class.forName(this.getClass().getGenericInterfaces()[0].getTypeName()).getGenericInterfaces()[0];
            final String className = ((Class<T>) pt.getActualTypeArguments()[0]).getSimpleName();
            return className;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Nelze automaticky zjistit název datového objektu, je nutné překrýt metodu getClassName!", e);
        }
    }
}
