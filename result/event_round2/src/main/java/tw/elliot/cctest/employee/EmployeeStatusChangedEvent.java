package tw.elliot.cctest.employee;

import java.util.UUID;

public record EmployeeStatusChangedEvent(UUID employeeId, String email, String oldStatus, String newStatus, UUID fromDepartmentId, UUID toDepartmentId) {
}
