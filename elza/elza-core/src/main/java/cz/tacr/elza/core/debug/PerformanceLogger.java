package cz.tacr.elza.core.debug;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "elza.debug", name = "performanceLogger", havingValue = "true")
public class PerformanceLogger {
	
    private final static Logger logger = LoggerFactory.getLogger(PerformanceLogger.class);
	
	public static class Performance {
		private final String method; 
		private long start = 0;
		
		public Performance(String method) {
			start = System.currentTimeMillis();
			this.method = method;
		}
		
		public long getStart() {
			return start;
		}
		
		public String getMethod() {
			return method;
		}
		
	}
	
    private ThreadLocal<Performance> performanceThreadLocal = new ThreadLocal<>();

    PerformanceLogger() {
        logger.info("Starting performance logger");
    }

	 /**
     * Obaluje všechny metody v managerech a loguje vstupy, výstupy a výjimky.
     */
	@Around("( @annotation(org.springframework.web.bind.annotation.RequestMapping) || @annotation(org.springframework.messaging.handler.annotation.MessageMapping) ) && execution(* cz.tacr.elza.controller.*.*(..)))")
    public Object handleStaleObjectStateException(ProceedingJoinPoint pjp) throws Throwable {
        String methodHeader = getMethodHeader(pjp);
        Performance performance = performanceThreadLocal.get();
        boolean topLevel = false;
        if ( performance == null ) {
        	topLevel = true;
        	performance = new Performance(methodHeader);
        }
                
        Object result = null;
        Throwable throwable = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
        	if ( topLevel ) {
                long timeIntervalMs = System.currentTimeMillis() - performance.getStart();
                StringBuilder sb = new StringBuilder();
                sb.append("Call time ").append(performance.getMethod()).append(":").append(timeIntervalMs).append("ms");
                if (throwable != null) {
                    sb.append(", throws exception: ").append(throwable.toString());
                }
                logger.info(sb.toString());
        	}
        }
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
    
    
    public Performance getPerformance() {
    	return performanceThreadLocal.get(); 
    }
	
}
