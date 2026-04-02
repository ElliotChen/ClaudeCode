package tw.elliot.cctest.employee;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import tw.elliot.cctest.employee.EmployeePromotedEvent;
import tw.elliot.cctest.employee.EmployeeDemotedEvent;
import tw.elliot.cctest.employee.EmployeeStatusChangedEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> findById(UUID id) {
        return employeeRepository.findById(id);
    }

    public List<Employee> findByDepartment(UUID departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public Employee hire(String name, String email, Rank rank, Status status, UUID departmentId) {
        log.info("Hiring employee: {} ({})", name, email);
        Employee employee = Employee.builder()
                .name(name)
                .email(email)
                .rank(rank)
                .status(status)
                .departmentId(departmentId)
                .build();
        return employeeRepository.save(employee);
    }

    public Employee promote(UUID employeeId) {
        log.info("Promoting employee with id: {}", employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        String oldRank = employee.getRank().name();
        employee.promote();
        String newRank = employee.getRank().name();

        Employee savedEmployee = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeePromotedEvent(
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                oldRank,
                newRank
        ));

        return savedEmployee;
    }

    public Employee demote(UUID employeeId) {
        log.info("Demoting employee with id: {}", employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        String oldRank = employee.getRank().name();
        employee.demote();
        String newRank = employee.getRank().name();

        Employee savedEmployee = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeeDemotedEvent(
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                oldRank,
                newRank
        ));

        return savedEmployee;
    }

    public Employee fire(UUID employeeId) {
        log.info("Firing employee with id: {}", employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        String oldStatus = employee.getStatus().name();
        UUID fromDepartmentId = employee.getDepartmentId();
        employee.terminate();

        Employee savedEmployee = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeeStatusChangedEvent(
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                oldStatus,
                Status.TERMINATED.name(),
                fromDepartmentId,
                null
        ));

        return savedEmployee;
    }

    public Employee transfer(UUID employeeId, UUID toDepartmentId) {
        log.info("Transferring employee {} to department {}", employeeId, toDepartmentId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        UUID fromDepartmentId = employee.getDepartmentId();
        String oldStatus = employee.getStatus().name();

        employee.assignToDepartment(toDepartmentId);

        Employee savedEmployee = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeeStatusChangedEvent(
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                oldStatus,
                employee.getStatus().name(),
                fromDepartmentId,
                toDepartmentId
        ));

        return savedEmployee;
    }
}
