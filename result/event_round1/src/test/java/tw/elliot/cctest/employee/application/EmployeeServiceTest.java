package tw.elliot.cctest.employee.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tw.elliot.cctest.employee.domain.Employee;
import tw.elliot.cctest.employee.domain.EmployeeRepository;
import tw.elliot.cctest.employee.domain.Rank;
import tw.elliot.cctest.employee.domain.Status;
import tw.elliot.cctest.employee.event.EmployeeHiredEvent;
import tw.elliot.cctest.employee.event.EmployeeFiredEvent;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private EmployeeService employeeService;

    @Test
    void hireShouldSaveEmployeeAndPublishEvent() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.hire(id, "Test", deptId, Rank.JUNIOR);

        assertEquals("Test", result.getName());
        assertEquals(Status.ACTIVE, result.getStatus());
        verify(employeeRepository).save(any(Employee.class));
        verify(eventPublisher).publishEvent(any(EmployeeHiredEvent.class));
    }

    @Test
    void fireShouldTerminateEmployeeAndPublishEvent() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", deptId, Rank.JUNIOR);

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        employeeService.fire(id);

        assertEquals(Status.TERMINATED, employee.getStatus());
        verify(employeeRepository).save(employee);
        verify(eventPublisher).publishEvent(any(EmployeeFiredEvent.class));
    }

    @Test
    void fireShouldThrowExceptionForNonExistentEmployee() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();

        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.fire(id));
    }

    @Test
    void transferShouldChangeDepartmentAndPublishEvent() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        UUID oldDeptId = UUID.randomUUID();
        UUID newDeptId = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", oldDeptId, Rank.JUNIOR);

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        employeeService.transfer(id, newDeptId);

        assertEquals(newDeptId, employee.getDepartmentId());
        verify(employeeRepository).save(employee);
    }

    @Test
    void promoteShouldIncreaseRankAndPublishEvent() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.JUNIOR);

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        employeeService.promote(id);

        assertEquals(Rank.MID, employee.getRank());
        verify(employeeRepository).save(employee);
    }

    @Test
    void demoteShouldDecreaseRankAndPublishEvent() {
        employeeService = new EmployeeService(employeeRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.MID);

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        employeeService.demote(id);

        assertEquals(Rank.JUNIOR, employee.getRank());
        verify(employeeRepository).save(employee);
    }
}
