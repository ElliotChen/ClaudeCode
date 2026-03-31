package tw.elliot.cctest.employee.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.employee.domain.Employee;
import tw.elliot.cctest.employee.domain.EmployeeRepository;
import tw.elliot.cctest.employee.domain.Rank;
import tw.elliot.cctest.employee.event.*;

import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeService(EmployeeRepository employeeRepository, ApplicationEventPublisher eventPublisher) {
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    public Employee hire(UUID id, String name, UUID departmentId, Rank initialRank) {
        Employee employee = new Employee(id, name, departmentId, initialRank);
        employeeRepository.save(employee);
        eventPublisher.publishEvent(EmployeeHiredEvent.of(id, name, departmentId));
        return employee;
    }

    public void fire(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.terminate();
        employeeRepository.save(employee);
        eventPublisher.publishEvent(EmployeeFiredEvent.of(employeeId, employee.getDepartmentId()));
    }

    public void transfer(UUID employeeId, UUID newDepartmentId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        UUID oldDepartmentId = employee.getDepartmentId();
        employee.transfer(newDepartmentId);
        employeeRepository.save(employee);
        eventPublisher.publishEvent(EmployeeTransferredEvent.of(employeeId, oldDepartmentId, newDepartmentId));
    }

    public void promote(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.promote();
        employeeRepository.save(employee);
        eventPublisher.publishEvent(EmployeePromotedEvent.of(employeeId, employee.getDepartmentId(), employee.getRank()));
    }

    public void demote(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.demote();
        employeeRepository.save(employee);
        eventPublisher.publishEvent(EmployeeDemotedEvent.of(employeeId, employee.getDepartmentId(), employee.getRank()));
    }
}
