package tw.elliot.cctest.employee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Employee Entity Tests")
class EmployeeTest {

    @Test
    @DisplayName("Employee creation with default rank (JUNIOR) and status (ACTIVE)")
    void shouldCreateEmployeeWithDefaultRankAndStatus() {
        // Given
        String name = "Test User";
        String email = "test@example.com";

        // When
        Employee employee = Employee.builder()
                .name(name)
                .email(email)
                .build();

        // Then
        assertThat(employee.getId()).isNotNull();
        assertThat(employee.getName()).isEqualTo(name);
        assertThat(employee.getEmail()).isEqualTo(email);
        assertThat(employee.getRank()).isEqualTo(Rank.JUNIOR);
        assertThat(employee.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(employee.getDepartmentId()).isNull();
    }

    @Test
    @DisplayName("Employee creation with specified rank and status")
    void shouldCreateEmployeeWithSpecifiedRankAndStatus() {
        // Given
        String name = "Test User";
        String email = "test@example.com";

        // When
        Employee employee = Employee.builder()
                .name(name)
                .email(email)
                .rank(Rank.SENIOR)
                .status(Status.ON_LEAVE)
                .build();

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.SENIOR);
        assertThat(employee.getStatus()).isEqualTo(Status.ON_LEAVE);
    }

    @Test
    @DisplayName("promote() increases rank")
    void shouldIncreaseRankWhenPromote() {
        // Given
        Employee employee = Employee.builder()
                .name("Test User")
                .email("test@example.com")
                .rank(Rank.MID)
                .build();

        // When
        employee.promote();

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.SENIOR);
    }

    @Test
    @DisplayName("promote() at PRINCIPAL does not change")
    void shouldNotChangeRankWhenPromoteAtPrincipal() {
        // Given
        Employee employee = Employee.builder()
                .name("Test User")
                .email("test@example.com")
                .rank(Rank.PRINCIPAL)
                .build();

        // When
        employee.promote();

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.PRINCIPAL);
    }

    @Test
    @DisplayName("demote() decreases rank")
    void shouldDecreaseRankWhenDemote() {
        // Given
        Employee employee = Employee.builder()
                .name("Test User")
                .email("test@example.com")
                .rank(Rank.SENIOR)
                .build();

        // When
        employee.demote();

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.MID);
    }

    @Test
    @DisplayName("demote() at JUNIOR does not change")
    void shouldNotChangeRankWhenDemoteAtJunior() {
        // Given
        Employee employee = Employee.builder()
                .name("Test User")
                .email("test@example.com")
                .rank(Rank.JUNIOR)
                .build();

        // When
        employee.demote();

        // Then
        assertThat(employee.getRank()).isEqualTo(Rank.JUNIOR);
    }

    @Test
    @DisplayName("terminate() sets status to TERMINATED and clears department")
    void shouldSetStatusToTerminatedAndClearDepartmentWhenTerminate() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Employee employee = Employee.builder()
                .name("Test User")
                .email("test@example.com")
                .departmentId(departmentId)
                .build();

        // When
        employee.terminate();

        // Then
        assertThat(employee.getStatus()).isEqualTo(Status.TERMINATED);
        assertThat(employee.getDepartmentId()).isNull();
    }
}
