package tw.elliot.cctest.employee.event;

import java.time.Instant;
import java.util.UUID;

public record EmployeeHiredEvent(
    UUID employeeId,
    String name,
    UUID departmentId,
    Instant occurredOn
) {
    public static EmployeeHiredEvent of(UUID employeeId, String name, UUID departmentId) {
        return new EmployeeHiredEvent(employeeId, name, departmentId, Instant.now());
    }
}
