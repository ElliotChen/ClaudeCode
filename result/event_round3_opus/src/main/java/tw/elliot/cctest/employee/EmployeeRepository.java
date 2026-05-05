package tw.elliot.cctest.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByDepartmentId(UUID departmentId);

    boolean existsByDepartmentIdAndRank(UUID departmentId, Rank rank);
}
