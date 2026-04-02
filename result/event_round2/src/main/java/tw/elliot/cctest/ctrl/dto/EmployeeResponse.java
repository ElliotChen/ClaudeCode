package tw.elliot.cctest.ctrl.dto;

import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    String name,
    String email,
    String rank,
    String status,
    UUID departmentId
) {
}
