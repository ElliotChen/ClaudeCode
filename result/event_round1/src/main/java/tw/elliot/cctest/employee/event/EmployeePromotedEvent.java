package tw.elliot.cctest.employee.event;

import tw.elliot.cctest.employee.domain.Rank;
import java.time.Instant;
import java.util.UUID;

public record EmployeePromotedEvent(
    UUID employeeId,
    UUID departmentId,
    Rank newRank,
    Instant occurredOn
) {
    public static EmployeePromotedEvent of(UUID employeeId, UUID departmentId, Rank newRank) {
        return new EmployeePromotedEvent(employeeId, departmentId, newRank, Instant.now());
    }
}
