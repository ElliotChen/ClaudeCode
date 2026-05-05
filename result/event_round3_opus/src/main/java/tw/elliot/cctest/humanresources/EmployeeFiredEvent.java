package tw.elliot.cctest.humanresources;

import java.util.UUID;

public record EmployeeFiredEvent(UUID employeeId, String name) {
}
