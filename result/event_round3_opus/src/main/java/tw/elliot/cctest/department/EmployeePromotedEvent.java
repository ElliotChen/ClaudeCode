package tw.elliot.cctest.department;

import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

public record EmployeePromotedEvent(UUID employeeId, UUID departmentId, Rank fromRank, Rank toRank) {
}
