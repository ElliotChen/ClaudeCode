package tw.elliot.cctest.department;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Department Integration Tests")
@SpringBootTest
@ActiveProfiles("test")
class DepartmentIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Department creation and repository save")
    void shouldCreateAndSaveDepartment() {
        // Given
        String name = "Test Department";
        String code = "TEST";

        Department department = Department.builder()
                .name(name)
                .code(code)
                .build();

        // When
        Department saved = departmentRepository.save(department);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo(name);
        assertThat(saved.getCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("Find department by code")
    void shouldFindDepartmentByCode() {
        // Given
        String code = "FIND_BY_CODE";
        Department department = Department.builder()
                .name("Find By Code")
                .code(code)
                .build();

        // When
        departmentRepository.save(department);
        Optional<Department> found = departmentRepository.findByCode(code);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Find By Code");
    }

    @Test
    @DisplayName("Find department by ID")
    void shouldFindDepartmentById() {
        // Given
        Department department = Department.builder()
                .name("Find By ID")
                .code("FIND_ID")
                .build();
        departmentRepository.save(department);

        // When
        Optional<Department> found = departmentRepository.findById(department.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("FIND_ID");
    }

    @Test
    @DisplayName("Delete department")
    void shouldDeleteDepartment() {
        // Given
        Department department = Department.builder()
                .name("Delete Me")
                .code("DELETE")
                .build();
        departmentRepository.save(department);

        // When
        departmentRepository.delete(department);
        Optional<Department> found = departmentRepository.findById(department.getId());

        // Then
        assertThat(found).isEmpty();
    }
}
