package cz.tacr.elza.aop;

import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logování metod.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 15. 9. 2015
 */
@Aspect
@Component
public class MethodLogger {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ThreadLocal<AtomicInteger> alreadyLogged = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    /**
     * Obaluje všechny metody v managerech a loguje vstupy, výstupy a výjimky.
     */
    @Around(value = "execution(* cz.tacr.elza.api.controller..*.*(..))")
    public Object handleStaleObjectStateException(ProceedingJoinPoint pjp) throws Throwable {
        String methodHeader = getMethodHeader(pjp);
        Object result = null;
        Throwable throwable = null;
        try {
            if (isTopLevel()) {
                logger.debug("Volání metody " + methodHeader);
            }
            alreadyLogged.get().incrementAndGet();
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            alreadyLogged.get().decrementAndGet();
            if (throwable == null) {
                if (result != null && isTopLevel()) {
                    logger.debug("Výsledek volání metody " + methodHeader + ": " + result);
                }
            } else if (isTopLevel() && !(throwable instanceof MethodLoggerIgnoreException)) {
                logger.error("Nastala chyba při volání metody " + methodHeader, throwable);
            }
        }
    }

    /**
     * Metoda kontroluje zda se loguje jen vstupní volání do manageru. Vnořená volání se nelogují.
     *
     * @return zda jsme ve vstupním volání
     */
    private boolean isTopLevel() {
        return alreadyLogged.get().intValue() == 0;
    }

    private String getMethodHeader(ProceedingJoinPoint pjp) {
        String methodName = pjp.getSignature().getName();
        Class<?> classs = pjp.getSignature().getDeclaringType();

        StringBuilder sb = new StringBuilder(classs.getSimpleName())
            .append(".")
            .append(methodName);

        Object[] args = pjp.getArgs();
        sb.append("(");
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg == null) {
                    sb.append("null");
                } else {
                    sb.append(arg);
                }
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }
}
