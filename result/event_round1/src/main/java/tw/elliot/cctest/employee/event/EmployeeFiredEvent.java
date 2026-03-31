package tw.elliot.cctest.employee.event;

import java.time.Instant;
import java.util.UUID;

public record EmployeeFiredEvent(
    UUID employeeId,
    UUID departmentId,
    Instant occurredOn
) {
    public static EmployeeFiredEvent of(UUID employeeId, UUID departmentId) {
        return new EmployeeFiredEvent(employeeId, departmentId, Instant.now());
    }
}
