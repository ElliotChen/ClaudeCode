package tw.elliot.cctest.department;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeService employeeService;
    private final ApplicationEventPublisher eventPublisher;

    public Department create(String name, String description) {
        var department = new Department(name, description);
        return departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public Department findById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> findEmployees(UUID departmentId) {
        findById(departmentId); // verify department exists
        return employeeService.findByDepartmentId(departmentId).stream()
                .map(EmployeeDto::from)
                .toList();
    }

    public EmployeeDto transfer(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        UUID fromDepartmentId = employee.getDepartmentId();
        Employee updated = employeeService.transfer(employeeId, departmentId);
        eventPublisher.publishEvent(new EmployeeTransferredEvent(employeeId, fromDepartmentId, departmentId));
        return EmployeeDto.from(updated);
    }

    public EmployeeDto promote(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        Rank fromRank = employee.getRank();
        Employee updated = employeeService.promote(employeeId);
        eventPublisher.publishEvent(new EmployeePromotedEvent(employeeId, departmentId, fromRank, updated.getRank()));
        return EmployeeDto.from(updated);
    }

    public EmployeeDto demote(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        Rank fromRank = employee.getRank();
        Employee updated = employeeService.demote(employeeId);
        eventPublisher.publishEvent(new EmployeeDemotedEvent(employeeId, departmentId, fromRank, updated.getRank()));
        return EmployeeDto.from(updated);
    }
}
