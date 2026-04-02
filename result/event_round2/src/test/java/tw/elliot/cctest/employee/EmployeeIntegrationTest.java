package tw.elliot.cctest.employee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Employee Integration Tests")
@SpringBootTest
@ActiveProfiles("test")
class EmployeeIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("Employee creation and repository save")
    void shouldCreateAndSaveEmployee() {
        // Given
        String name = "Test Employee";
        String email = "test.employee@example.com";

        Employee employee = Employee.builder()
                .name(name)
                .email(email)
                .build();

        // When
        Employee savedEmployee = employeeRepository.save(employee);

        // Then
        assertThat(savedEmployee.getId()).isNotNull();
        assertThat(savedEmployee.getName()).isEqualTo(name);
        assertThat(savedEmployee.getEmail()).isEqualTo(email);
        assertThat(savedEmployee.getRank()).isEqualTo(Rank.JUNIOR);
        assertThat(savedEmployee.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("Find employee by email")
    void shouldFindEmployeeByEmail() {
        // Given
        String email = "find.by.email@example.com";
        Employee employee = Employee.builder()
                .name("Find By Email Test")
                .email(email)
                .build();

        // When
        employeeRepository.save(employee);
        Optional<Employee> found = employeeRepository.findByEmail(email);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Find By Email Test");
    }

    @Test
    @DisplayName("Find employees by department ID")
    void shouldFindEmployeesByDepartment() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Employee emp1 = Employee.builder().name("Emp 1").email("emp1@test.com").departmentId(departmentId).build();
        Employee emp2 = Employee.builder().name("Emp 2").email("emp2@test.com").departmentId(departmentId).build();
        Employee emp3 = Employee.builder().name("Emp 3").email("emp3@test.com").build();

        // When
        employeeRepository.save(emp1);
        employeeRepository.save(emp2);
        employeeRepository.save(emp3);

        // Then
        assertThat(employeeRepository.findByDepartmentId(departmentId)).hasSize(2);
    }

    @Test
    @DisplayName("Promote employee")
    void shouldPromoteEmployee() {
        // Given
        Employee employee = Employee.builder()
                .name("Promote Test")
                .email("promote@test.com")
                .rank(Rank.JUNIOR)
                .build();
        employeeRepository.save(employee);

        // When
        employee.promote();
        employeeRepository.save(employee);

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.MID);
    }

    @Test
    @DisplayName("Terminate employee")
    void shouldTerminateEmployee() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Employee employee = Employee.builder()
                .name("Terminate Test")
                .email("terminate@test.com")
                .departmentId(departmentId)
                .status(Status.ACTIVE)
                .build();
        employeeRepository.save(employee);

        // When
        employee.terminate();
        employeeRepository.save(employee);

        // Then
        assertThat(employee.getStatus()).isEqualTo(Status.TERMINATED);
        assertThat(employee.getDepartmentId()).isNull();
    }
}
