package tw.elliot.cctest.employee;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Employee create(String name, String email, String phone, UUID departmentId) {
        var employee = new Employee(name, email, phone, departmentId);
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Employee> findByDepartmentId(UUID departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public Employee promote(UUID employeeId) {
        var employee = findById(employeeId);
        Rank newRank = employee.getRank().promote();
        if (newRank == Rank.MANAGER && employee.getDepartmentId() != null) {
            if (employeeRepository.existsByDepartmentIdAndRank(employee.getDepartmentId(), Rank.MANAGER)) {
                throw new IllegalStateException("Department already has a MANAGER");
            }
        }
        employee.setRank(newRank);
        return employeeRepository.save(employee);
    }

    public Employee demote(UUID employeeId) {
        var employee = findById(employeeId);
        Rank newRank = employee.getRank().demote();
        employee.setRank(newRank);
        return employeeRepository.save(employee);
    }

    public Employee transfer(UUID employeeId, UUID toDepartmentId) {
        var employee = findById(employeeId);
        if (employee.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Cannot transfer INACTIVE employee");
        }
        if (employee.getRank() == Rank.MANAGER) {
            if (employeeRepository.existsByDepartmentIdAndRank(toDepartmentId, Rank.MANAGER)) {
                throw new IllegalStateException("Target department already has a MANAGER");
            }
        }
        employee.setDepartmentId(toDepartmentId);
        return employeeRepository.save(employee);
    }

    public Employee fire(UUID employeeId) {
        var employee = findById(employeeId);
        employee.setStatus(Status.INACTIVE);
        employee.setDepartmentId(null);
        return employeeRepository.save(employee);
    }
}
