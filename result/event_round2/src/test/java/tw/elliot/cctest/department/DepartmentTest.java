package tw.elliot.cctest.department;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Department Entity Tests")
class DepartmentTest {

    @Test
    @DisplayName("Department creation with generated UUID id")
    void shouldCreateDepartmentWithGeneratedUuid() {
        // When
        Department department = Department.builder()
                .name("Engineering")
                .code("ENG")
                .build();

        // Then
        assertThat(department.getId()).isNotNull();
    }

    @Test
    @DisplayName("Department name and code are set correctly")
    void shouldSetDepartmentNameAndCode() {
        // Given
        String name = "Engineering";
        String code = "ENG";

        // When
        Department department = Department.builder()
                .name(name)
                .code(code)
                .build();

        // Then
        assertThat(department.getName()).isEqualTo(name);
        assertThat(department.getCode()).isEqualTo(code);
    }
}
