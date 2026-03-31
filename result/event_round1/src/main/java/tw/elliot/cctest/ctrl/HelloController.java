package tw.elliot.cctest.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.tracing.Tracer;

@RestController
@RequestMapping("/ctrl")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    private final Tracer tracer;

    public HelloController(Tracer tracer) {
        this.tracer = tracer;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        log.debug("Handling /ctrl/hello request");

        String traceId = tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId()
            : "no-trace";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", traceId);

        return ResponseEntity.ok()
            .headers(headers)
            .body("Hello, World!");
    }
}
