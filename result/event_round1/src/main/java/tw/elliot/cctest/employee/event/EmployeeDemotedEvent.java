package tw.elliot.cctest.employee.event;

import tw.elliot.cctest.employee.domain.Rank;
import java.time.Instant;
import java.util.UUID;

public record EmployeeDemotedEvent(
    UUID employeeId,
    UUID departmentId,
    Rank newRank,
    Instant occurredOn
) {
    public static EmployeeDemotedEvent of(UUID employeeId, UUID departmentId, Rank newRank) {
        return new EmployeeDemotedEvent(employeeId, departmentId, newRank, Instant.now());
    }
}
