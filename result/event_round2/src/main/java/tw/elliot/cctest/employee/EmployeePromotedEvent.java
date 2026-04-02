package tw.elliot.cctest.employee;

import java.util.UUID;

public record EmployeePromotedEvent(UUID employeeId, String email, String oldRank, String newRank) {
}
