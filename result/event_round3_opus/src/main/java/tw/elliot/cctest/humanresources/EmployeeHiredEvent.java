package tw.elliot.cctest.humanresources;

import java.util.UUID;

public record EmployeeHiredEvent(UUID employeeId, UUID departmentId, String name) {
}
