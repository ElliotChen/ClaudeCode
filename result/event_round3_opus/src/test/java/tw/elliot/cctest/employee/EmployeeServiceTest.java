package tw.elliot.cctest.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
    }

    private void stubSave() {
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void promote_fromStaff_shouldBecomeTeamLead() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        stubSave();

        var result = employeeService.promote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void promote_fromTeamLead_shouldBecomeManager() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.TEAM_LEAD);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(departmentId, Rank.MANAGER)).thenReturn(false);
        stubSave();

        var result = employeeService.promote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.MANAGER);
    }

    @Test
    void promote_fromManager_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.promote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot promote beyond MANAGER");
    }

    @Test
    void promote_toManager_whenDeptAlreadyHasManager_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.TEAM_LEAD);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(departmentId, Rank.MANAGER)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.promote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Department already has a MANAGER");
    }

    @Test
    void demote_fromManager_shouldBecomeTeamLead() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        stubSave();

        var result = employeeService.demote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void demote_fromStaff_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.demote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot demote below STAFF");
    }

    @Test
    void transfer_activeEmployee_shouldChangeDepartment() {
        var oldDeptId = UUID.randomUUID();
        var newDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, oldDeptId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        stubSave();

        var result = employeeService.transfer(employee.getId(), newDeptId);

        assertThat(result.getDepartmentId()).isEqualTo(newDeptId);
    }

    @Test
    void transfer_inactiveEmployee_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setStatus(Status.INACTIVE);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.transfer(employee.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transfer INACTIVE employee");
    }

    @Test
    void transfer_managerToDeptWithManager_shouldThrow() {
        var newDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(newDeptId, Rank.MANAGER)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.transfer(employee.getId(), newDeptId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Target department already has a MANAGER");
    }

    @Test
    void fire_shouldSetInactiveAndClearDepartment() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        stubSave();

        var result = employeeService.fire(employee.getId());

        assertThat(result.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getRank()).isEqualTo(Rank.STAFF);
    }
}
