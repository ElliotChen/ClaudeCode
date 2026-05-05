package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tw.elliot.cctest.department.Department;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.department.EmployeePromotedEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrServiceTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private HrRecordRepository hrRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private HrService hrService;

    private UUID departmentId;

    private void stubSaveRecord() {
        when(hrRecordRepository.save(any(HrRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
    }

    @Test
    void hire_shouldCreateEmployeeAndRecord() {
        var department = new Department("Engineering", "Eng");
        var employee = new Employee("Jane", "jane@test.com", "123", departmentId);

        when(departmentService.findById(departmentId)).thenReturn(department);
        when(employeeService.create("Jane", "jane@test.com", "123", departmentId)).thenReturn(employee);
        stubSaveRecord();

        var result = hrService.hire("Jane", "jane@test.com", "123", departmentId);

        assertThat(result.name()).isEqualTo("Jane");
        assertThat(result.rank()).isEqualTo(Rank.STAFF);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.HIRED);

        verify(eventPublisher).publishEvent(any(EmployeeHiredEvent.class));
    }

    @Test
    void fire_shouldSetInactiveAndCreateRecord() {
        var employee = new Employee("Jane", "jane@test.com", null, departmentId);
        employee.setStatus(tw.elliot.cctest.employee.Status.INACTIVE);
        employee.setDepartmentId(null);

        when(employeeService.fire(employee.getId())).thenReturn(employee);
        stubSaveRecord();

        var result = hrService.fire(employee.getId());

        assertThat(result.status()).isEqualTo(tw.elliot.cctest.employee.Status.INACTIVE);
        assertThat(result.departmentId()).isNull();

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.FIRED);

        verify(eventPublisher).publishEvent(any(EmployeeFiredEvent.class));
    }

    @Test
    void onTransferredEvent_shouldCreateRecord() {
        var event = new EmployeeTransferredEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stubSaveRecord();

        hrService.on(event);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.TRANSFERRED);
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(event.employeeId());
    }

    @Test
    void onPromotedEvent_shouldCreateRecord() {
        var event = new EmployeePromotedEvent(UUID.randomUUID(), departmentId, Rank.STAFF, Rank.TEAM_LEAD);
        stubSaveRecord();

        hrService.on(event);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.PROMOTED);
        assertThat(captor.getValue().getDetail()).contains("STAFF").contains("TEAM_LEAD");
    }
}
