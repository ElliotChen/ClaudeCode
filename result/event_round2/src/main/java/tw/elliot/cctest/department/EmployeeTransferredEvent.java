package tw.elliot.cctest.department;

import java.util.UUID;

public record EmployeeTransferredEvent(UUID employeeId, String email, UUID fromDepartmentId, UUID toDepartmentId) {
}
