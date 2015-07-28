/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tacr.elza.ax;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

import cz.tacr.elza.ax.*;


/**
 * @author ondrej
 */
public class AxRepositoryFactoryBean<R extends JpaRepository<T, I>, T extends IdObject<I>, I extends Integer>
        extends JpaRepositoryFactoryBean<R, T, I> {

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new AxRepositoryFactory(entityManager);
    }

    public static class AxRepositoryFactory<T extends IdObject<I>, I extends Serializable> extends JpaRepositoryFactory {

        private EntityManager entityManager;

        public AxRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
            this.entityManager = entityManager;
        }

        @Override
        protected Object getTargetRepository(RepositoryMetadata metadata) {
            return new AxRepositoryImpl(metadata.getDomainType(), entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            return cz.tacr.elza.ax.AxRepository.class;
        }
    }
}
