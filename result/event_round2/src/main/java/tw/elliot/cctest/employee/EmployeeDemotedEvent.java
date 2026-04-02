package tw.elliot.cctest.employee;

import java.util.UUID;

public record EmployeeDemotedEvent(UUID employeeId, String email, String oldRank, String newRank) {
}
