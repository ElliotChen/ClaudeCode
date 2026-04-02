package tw.elliot.cctest.ctrl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.ctrl.dto.EmployeeResponse;
import tw.elliot.cctest.ctrl.dto.HireEmployeeRequest;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.Status;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/employees")
    public List<Employee> getAllEmployees() {
        return employeeService.findAll();
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable UUID id) {
        return employeeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/departments/{deptId}/employees")
    public ResponseEntity<Employee> hireEmployee(
            @PathVariable UUID deptId,
            @RequestBody HireEmployeeRequest request
    ) {
        Employee employee = employeeService.hire(
                request.name(),
                request.email(),
                Rank.JUNIOR,
                Status.ACTIVE,
                deptId
        );
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/employees/{id}/promote")
    public ResponseEntity<Employee> promoteEmployee(@PathVariable UUID id) {
        Employee promoted = employeeService.promote(id);
        return ResponseEntity.ok(promoted);
    }

    @PostMapping("/employees/{id}/demote")
    public ResponseEntity<Employee> demoteEmployee(@PathVariable UUID id) {
        Employee demoted = employeeService.demote(id);
        return ResponseEntity.ok(demoted);
    }

    @PostMapping("/employees/{id}/fire")
    public ResponseEntity<Employee> fireEmployee(@PathVariable UUID id) {
        Employee fired = employeeService.fire(id);
        return ResponseEntity.ok(fired);
    }

    @PostMapping("/employees/{id}/transfer")
    public ResponseEntity<Employee> transferEmployee(
            @PathVariable UUID id,
            @RequestParam UUID toDept
    ) {
        Employee transferred = employeeService.transfer(id, toDept);
        return ResponseEntity.ok(transferred);
    }
}
