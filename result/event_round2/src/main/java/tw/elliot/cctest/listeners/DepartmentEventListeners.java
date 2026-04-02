package tw.elliot.cctest.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import tw.elliot.cctest.department.EmployeeHiredEvent;
import tw.elliot.cctest.department.EmployeeFiredEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentEventListeners {

    @ApplicationModuleListener
    void on(EmployeeHiredEvent event) {
        log.info("Employee hired event received: employeeId={}, email={}, departmentId={}",
                event.employeeId(), event.email(), event.departmentId());
    }

    @ApplicationModuleListener
    void on(EmployeeFiredEvent event) {
        log.info("Employee fired event received: employeeId={}, email={}, departmentId={}",
                event.employeeId(), event.email(), event.departmentId());
    }

    @ApplicationModuleListener
    void on(EmployeeTransferredEvent event) {
        log.info("Employee transferred event received: employeeId={}, email={}, fromDepartmentId={}, toDepartmentId={}",
                event.employeeId(), event.email(), event.fromDepartmentId(), event.toDepartmentId());
    }
}
