package tw.elliot.cctest.employee.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.employee.application.EmployeeService;
import tw.elliot.cctest.employee.domain.Employee;
import tw.elliot.cctest.employee.domain.Rank;
import io.micrometer.tracing.Tracer;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee", description = "Employee management API")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final Tracer tracer;

    public EmployeeController(EmployeeService employeeService, Tracer tracer) {
        this.employeeService = employeeService;
        this.tracer = tracer;
    }

    @PostMapping
    @Operation(summary = "Hire a new employee")
    public ResponseEntity<Map<String, Object>> hire(@RequestBody HireRequest request) {
        log.debug("Hiring employee: {}", request.name());
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeService.hire(employeeId, request.name(), request.departmentId(), request.rank());

        String traceId = tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId()
            : "no-trace";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", traceId);

        return ResponseEntity.ok()
            .headers(headers)
            .body(Map.of(
                "traceId", traceId,
                "employeeId", employee.getId(),
                "name", employee.getName(),
                "departmentId", employee.getDepartmentId(),
                "rank", employee.getRank().name(),
                "status", employee.getStatus().name()
            ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fire an employee")
    public ResponseEntity<Void> fire(@PathVariable UUID id) {
        log.debug("Firing employee: {}", id);
        employeeService.fire(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/transfer")
    @Operation(summary = "Transfer employee to another department")
    public ResponseEntity<Void> transfer(@PathVariable UUID id, @RequestBody TransferRequest request) {
        log.debug("Transferring employee {} to department {}", id, request.departmentId());
        employeeService.transfer(id, request.departmentId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/promote")
    @Operation(summary = "Promote employee")
    public ResponseEntity<Void> promote(@PathVariable UUID id) {
        log.debug("Promoting employee: {}", id);
        employeeService.promote(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/demote")
    @Operation(summary = "Demote employee")
    public ResponseEntity<Void> demote(@PathVariable UUID id) {
        log.debug("Demoting employee: {}", id);
        employeeService.demote(id);
        return ResponseEntity.ok().build();
    }

    public record HireRequest(String name, UUID departmentId, Rank rank) {}
    public record TransferRequest(UUID departmentId) {}
}
