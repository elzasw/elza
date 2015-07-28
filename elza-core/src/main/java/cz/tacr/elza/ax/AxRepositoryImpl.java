/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tacr.elza.ax;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.PersistenceProvider;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;

import cz.tacr.elza.ax.*;


/**
 * @author ondrej
 */
public class AxRepositoryImpl<T extends IdObject<Integer>>
        extends SimpleJpaRepository<T, Integer>
        implements cz.tacr.elza.ax.AxRepository<T> {

    Class<T> entityClass;

    public AxRepositoryImpl(JpaEntityInformation<T, Integer> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        entityClass = entityInformation.getJavaType();
    }

    public AxRepositoryImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        entityClass = domainClass;
    }

    EntityManager getEntityManager() {
        return (EntityManager) readPrivateField("em");
    }

    PersistenceProvider getPersistenceProvider() {
        return (PersistenceProvider) readPrivateField("provider");
    }

    private Object readPrivateField(String declaredField) {
        try {
            Field field = SimpleJpaRepository.class.getDeclaredField(declaredField);
            field.setAccessible(true);
            return field.get(this);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot read field " + declaredField + " from SimpleJpaRepository");
        }
    }

    @Override
    public Class<T> entityClass() {
        return entityClass;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public T entityInstance() {
        try {
            return entityClass.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Chyba instance " + entityClass, ex);
        }
    }

    /*@Override
    public T saveMerge(T entity) {
        Assert.notNull(entity);
        if (!getEntityManager().contains(entity)) {
            entity = getEntityManager().merge(entity);
        }
        return save(entity);
    }*/

}
