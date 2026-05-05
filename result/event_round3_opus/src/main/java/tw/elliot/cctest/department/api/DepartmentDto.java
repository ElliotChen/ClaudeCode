package tw.elliot.cctest.department.api;

import tw.elliot.cctest.department.Department;

import java.util.UUID;

public record DepartmentDto(UUID id, String name, String description) {

    public static DepartmentDto from(Department department) {
        return new DepartmentDto(department.getId(), department.getName(), department.getDescription());
    }
}
