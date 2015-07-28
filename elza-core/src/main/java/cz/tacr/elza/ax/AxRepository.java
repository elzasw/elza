package cz.tacr.elza.ax;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import cz.tacr.elza.ax.IdObject;


/**
 * @author ondrej
 */
@NoRepositoryBean
public interface AxRepository<T extends IdObject<Integer>> extends JpaRepository<T, Integer> {

    Class<T> entityClass();

    T entityInstance();

    /*AxContainer<T> newContainer();*/
}
