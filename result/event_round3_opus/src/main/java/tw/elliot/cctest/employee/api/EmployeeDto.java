package tw.elliot.cctest.employee.api;

import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.Status;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDto(
        UUID id,
        String name,
        String email,
        String phone,
        Rank rank,
        Status status,
        UUID departmentId,
        LocalDate hireDate
) {
    public static EmployeeDto from(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRank(),
                employee.getStatus(),
                employee.getDepartmentId(),
                employee.getHireDate()
        );
    }
}
