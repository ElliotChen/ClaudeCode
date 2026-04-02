package tw.elliot.cctest.department;

import java.util.UUID;

public record EmployeeFiredEvent(UUID employeeId, String email, UUID departmentId) {
}
