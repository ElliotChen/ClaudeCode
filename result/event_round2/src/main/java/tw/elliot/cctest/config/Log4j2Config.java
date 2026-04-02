package tw.elliot.cctest.config;

import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.ThreadContext;

@Aspect
@Component
public class Log4j2Config {

    private final Tracer tracer;

    public Log4j2Config(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object traceRestController(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = tracer.currentTraceContext().context().traceId();
        String spanId = tracer.currentTraceContext().context().spanId();

        ThreadContext.put("traceId", traceId);
        ThreadContext.put("spanId", spanId);

        try {
            return joinPoint.proceed();
        } finally {
            ThreadContext.clearAll();
        }
    }
}
