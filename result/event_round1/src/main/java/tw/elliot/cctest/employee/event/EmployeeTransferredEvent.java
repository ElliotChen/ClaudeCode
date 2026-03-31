package tw.elliot.cctest.employee.event;

import java.time.Instant;
import java.util.UUID;

public record EmployeeTransferredEvent(
    UUID employeeId,
    UUID fromDepartmentId,
    UUID toDepartmentId,
    Instant occurredOn
) {
    public static EmployeeTransferredEvent of(UUID employeeId, UUID fromDepartmentId, UUID toDepartmentId) {
        return new EmployeeTransferredEvent(employeeId, fromDepartmentId, toDepartmentId, Instant.now());
    }
}
