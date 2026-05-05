package tw.elliot.cctest.department;

import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

public record EmployeeDemotedEvent(UUID employeeId, UUID departmentId, Rank fromRank, Rank toRank) {
}
