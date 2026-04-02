package tw.elliot.cctest.ctrl.dto;

import java.util.UUID;

public record DepartmentResponse(
    UUID id,
    String name,
    String code
) {
}
