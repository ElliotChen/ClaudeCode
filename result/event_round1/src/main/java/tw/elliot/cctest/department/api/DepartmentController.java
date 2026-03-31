package tw.elliot.cctest.department.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.department.application.DepartmentService;
import tw.elliot.cctest.department.domain.Department;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Department", description = "Department management API")
public class DepartmentController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @Operation(summary = "Create a new department")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateRequest request) {
        log.debug("Creating department: {}", request.name());
        UUID id = UUID.randomUUID();
        Department department = departmentService.create(id, request.name(), request.description());
        return ResponseEntity.ok(Map.of(
            "id", department.getId(),
            "name", department.getName(),
            "description", department.getDescription()
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @RequestBody UpdateRequest request) {
        log.debug("Updating department: {}", id);
        Department department = departmentService.update(id, request.name(), request.description());
        return ResponseEntity.ok(Map.of(
            "id", department.getId(),
            "name", department.getName(),
            "description", department.getDescription()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        log.debug("Getting department: {}", id);
        Department department = departmentService.getById(id);
        return ResponseEntity.ok(Map.of(
            "id", department.getId(),
            "name", department.getName(),
            "description", department.getDescription()
        ));
    }

    @GetMapping
    @Operation(summary = "Get all departments")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        log.debug("Getting all departments");
        List<Department> departments = departmentService.getAll();
        return ResponseEntity.ok(departments.stream()
            .map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "name", d.getName(),
                "description", d.getDescription()
            ))
            .toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.debug("Deleting department: {}", id);
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(String name, String description) {}
    public record UpdateRequest(String name, String description) {}
}
