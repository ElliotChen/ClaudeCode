package tw.elliot.cctest.humanresources.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.employee.api.EmployeeDto;
import tw.elliot.cctest.humanresources.HrService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Human Resources", description = "HR management — hire, fire, records")
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @Operation(summary = "Hire an employee into a department")
    @ApiResponse(responseCode = "201", description = "Employee hired")
    @PostMapping("/employees/hire")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDto hire(@RequestBody HireRequest request) {
        return hrService.hire(request.name(), request.email(), request.phone(), request.departmentId());
    }

    @Operation(summary = "Fire an employee")
    @ApiResponse(responseCode = "200", description = "Employee fired")
    @PostMapping("/employees/{id}/fire")
    public EmployeeDto fire(@PathVariable UUID id) {
        return hrService.fire(id);
    }

    @Operation(summary = "List HR records, optionally filtered by employeeId")
    @GetMapping("/records")
    public List<HrRecordDto> findRecords(@RequestParam(required = false) UUID employeeId) {
        if (employeeId != null) {
            return hrService.findRecordsByEmployeeId(employeeId).stream()
                    .map(HrRecordDto::from)
                    .toList();
        }
        return hrService.findAllRecords().stream()
                .map(HrRecordDto::from)
                .toList();
    }

    public record HireRequest(String name, String email, String phone, UUID departmentId) {
    }
}
