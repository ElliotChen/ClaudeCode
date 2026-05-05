package tw.elliot.cctest.department;

import java.util.UUID;

public record EmployeeTransferredEvent(UUID employeeId, UUID fromDepartmentId, UUID toDepartmentId) {
}
