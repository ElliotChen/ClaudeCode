package tw.elliot.cctest.department;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.department.EmployeeFiredEvent;
import tw.elliot.cctest.department.EmployeeHiredEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.Status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeService employeeService;
    private final ApplicationEventPublisher eventPublisher;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findById(UUID id) {
        return departmentRepository.findById(id);
    }

    public Department create(String name, String code) {
        log.info("Creating department: {} ({})", name, code);
        Department department = Department.builder()
                .name(name)
                .code(code)
                .build();
        return departmentRepository.save(department);
    }

    public Employee hireEmployee(String name, String email, Rank rank, Status status, UUID departmentId) {
        log.info("Hiring employee {} to department {}", name, departmentId);
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        Employee employee = employeeService.hire(name, email, rank, status, departmentId);

        eventPublisher.publishEvent(new EmployeeHiredEvent(
                employee.getId(),
                employee.getEmail(),
                departmentId
        ));

        return employee;
    }

    public Employee fireEmployee(UUID employeeId) {
        log.info("Firing employee with id: {}", employeeId);
        Employee employee = employeeService.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        UUID departmentId = employee.getDepartmentId();

        Employee firedEmployee = employeeService.fire(employeeId);

        eventPublisher.publishEvent(new EmployeeFiredEvent(
                firedEmployee.getId(),
                firedEmployee.getEmail(),
                departmentId
        ));

        return firedEmployee;
    }

    public Employee transferEmployee(UUID employeeId, UUID toDepartmentId) {
        log.info("Transferring employee {} to department {}", employeeId, toDepartmentId);
        Employee employee = employeeService.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        Department toDepartment = departmentRepository.findById(toDepartmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + toDepartmentId));

        UUID fromDepartmentId = employee.getDepartmentId();

        Employee transferredEmployee = employeeService.transfer(employeeId, toDepartmentId);

        eventPublisher.publishEvent(new EmployeeTransferredEvent(
                transferredEmployee.getId(),
                transferredEmployee.getEmail(),
                fromDepartmentId,
                toDepartmentId
        ));

        return transferredEmployee;
    }
}
