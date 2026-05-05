package tw.elliot.cctest.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private UUID departmentId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Engineering dept");
        departmentId = department.getId();
        employeeId = UUID.randomUUID();
    }

    @Test
    void transfer_shouldPublishTransferredEvent() {
        var oldDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, oldDeptId);
        employeeId = employee.getId();
        var updatedEmployee = new Employee("John", "john@test.com", null, departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.transfer(employeeId, departmentId)).thenReturn(updatedEmployee);

        departmentService.transfer(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeeTransferredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.employeeId()).isEqualTo(employeeId);
        assertThat(event.fromDepartmentId()).isEqualTo(oldDeptId);
        assertThat(event.toDepartmentId()).isEqualTo(departmentId);
    }

    @Test
    void promote_shouldPublishPromotedEvent() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employeeId = employee.getId();
        var promotedEmployee = new Employee("John", "john@test.com", null, departmentId);
        promotedEmployee.setRank(Rank.TEAM_LEAD);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.promote(employeeId)).thenReturn(promotedEmployee);

        departmentService.promote(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeePromotedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.employeeId()).isEqualTo(employeeId);
        assertThat(event.fromRank()).isEqualTo(Rank.STAFF);
        assertThat(event.toRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void demote_shouldPublishDemotedEvent() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employeeId = employee.getId();
        employee.setRank(Rank.TEAM_LEAD);
        var demotedEmployee = new Employee("John", "john@test.com", null, departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.demote(employeeId)).thenReturn(demotedEmployee);

        departmentService.demote(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeeDemotedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.fromRank()).isEqualTo(Rank.TEAM_LEAD);
        assertThat(event.toRank()).isEqualTo(Rank.STAFF);
    }

    @Test
    void create_shouldSaveDepartment() {
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = departmentService.create("Sales", "Sales dept");

        assertThat(result.getName()).isEqualTo("Sales");
        verify(departmentRepository).save(any(Department.class));
    }
}
