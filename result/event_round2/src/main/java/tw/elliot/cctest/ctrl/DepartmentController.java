package tw.elliot.cctest.ctrl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.ctrl.dto.CreateDepartmentRequest;
import tw.elliot.cctest.department.Department;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.employee.Employee;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/departments")
    public List<Department> getAllDepartments() {
        return departmentService.findAll();
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable UUID id) {
        return departmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departments/{id}/employees")
    public List<Employee> getDepartmentEmployees(@PathVariable UUID id) {
        return List.of();
    }

    @PostMapping("/departments")
    public ResponseEntity<Department> createDepartment(@RequestBody CreateDepartmentRequest request) {
        Department department = departmentService.create(request.name(), request.code());
        return ResponseEntity.ok(department);
    }
}
