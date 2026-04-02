package tw.elliot.cctest.ctrl;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HelloController {

    private final Tracer tracer;

    @GetMapping("/ctrl/hello")
    public String hello() {
        String traceId = tracer.currentTraceContext().context().traceId();
        log.info("Hello endpoint called with traceId: {}", traceId);
        return "Hello, World!";
    }
}
