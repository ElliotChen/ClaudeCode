package tw.elliot.cctest.department.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Tag(name = "Department", description = "Department management")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Create a department")
    @ApiResponse(responseCode = "201", description = "Department created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDto create(@RequestBody CreateDepartmentRequest request) {
        var department = departmentService.create(request.name(), request.description());
        return DepartmentDto.from(department);
    }

    @Operation(summary = "List all departments")
    @GetMapping
    public List<DepartmentDto> findAll() {
        return departmentService.findAll().stream()
                .map(DepartmentDto::from)
                .toList();
    }

    @Operation(summary = "Get department by ID")
    @ApiResponse(responseCode = "200", description = "Department found")
    @GetMapping("/{id}")
    public DepartmentDto findById(@PathVariable UUID id) {
        return DepartmentDto.from(departmentService.findById(id));
    }

    @Operation(summary = "List employees in a department")
    @GetMapping("/{id}/employees")
    public List<EmployeeDto> findEmployees(@PathVariable UUID id) {
        return departmentService.findEmployees(id);
    }

    @Operation(summary = "Transfer employee to this department")
    @ApiResponse(responseCode = "200", description = "Employee transferred")
    @PostMapping("/{id}/employees/{employeeId}/transfer")
    public EmployeeDto transfer(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.transfer(id, employeeId);
    }

    @Operation(summary = "Promote employee")
    @ApiResponse(responseCode = "200", description = "Employee promoted")
    @PostMapping("/{id}/employees/{employeeId}/promote")
    public EmployeeDto promote(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.promote(id, employeeId);
    }

    @Operation(summary = "Demote employee")
    @ApiResponse(responseCode = "200", description = "Employee demoted")
    @PostMapping("/{id}/employees/{employeeId}/demote")
    public EmployeeDto demote(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.demote(id, employeeId);
    }

    public record CreateDepartmentRequest(String name, String description) {
    }
}
