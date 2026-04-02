package tw.elliot.cctest.department;

import java.util.UUID;

public record EmployeeHiredEvent(UUID employeeId, String email, UUID departmentId) {
}
