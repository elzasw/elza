package cz.tacr.elza.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import cz.tacr.elza.api.exception.ConcurrentUpdateException;

/**
 * Konvertuje Hibernate výjimku na naší aby mohla být uvedena v api a nemusela tam být závislost na Hibernate.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
@Aspect
@Component
public class OptimisticLockingFailureExceptionHandler {

    /**
     * Obaluje všechny metody v managerech a převádí výjimku {@link OptimisticLockingFailureException}
     * na {@link ConcurrentUpdateException}.
     */
    @Around(value = "execution(* cz.tacr.elza.api.controller..*.*(..))")
    public Object handleStaleObjectStateException(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException(e);
        }
    }
}
