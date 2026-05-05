package tw.elliot.cctest.humanresources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.department.EmployeeDemotedEvent;
import tw.elliot.cctest.department.EmployeePromotedEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HrService {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final HrRecordRepository hrRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeDto hire(String name, String email, String phone, UUID departmentId) {
        departmentService.findById(departmentId); // verify department exists
        Employee employee = employeeService.create(name, email, phone, departmentId);
        hrRecordRepository.save(new HrRecord(employee.getId(), ActionType.HIRED,
                "Hired " + name + " into department " + departmentId));
        eventPublisher.publishEvent(new EmployeeHiredEvent(employee.getId(), departmentId, name));
        log.info("Hired employee {} into department {}", employee.getId(), departmentId);
        return EmployeeDto.from(employee);
    }

    public EmployeeDto fire(UUID employeeId) {
        Employee employee = employeeService.fire(employeeId);
        hrRecordRepository.save(new HrRecord(employeeId, ActionType.FIRED,
                "Fired " + employee.getName()));
        eventPublisher.publishEvent(new EmployeeFiredEvent(employeeId, employee.getName()));
        log.info("Fired employee {}", employeeId);
        return EmployeeDto.from(employee);
    }

    @Transactional(readOnly = true)
    public List<HrRecord> findAllRecords() {
        return hrRecordRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<HrRecord> findRecordsByEmployeeId(UUID employeeId) {
        return hrRecordRepository.findByEmployeeId(employeeId);
    }

    @ApplicationModuleListener
    void on(EmployeeTransferredEvent event) {
        log.info("Recording transfer for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.TRANSFERRED,
                "Transferred from department " + event.fromDepartmentId() + " to " + event.toDepartmentId()));
    }

    @ApplicationModuleListener
    void on(EmployeePromotedEvent event) {
        log.info("Recording promotion for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.PROMOTED,
                "Promoted from " + event.fromRank() + " to " + event.toRank() + " in department " + event.departmentId()));
    }

    @ApplicationModuleListener
    void on(EmployeeDemotedEvent event) {
        log.info("Recording demotion for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.DEMOTED,
                "Demoted from " + event.fromRank() + " to " + event.toRank() + " in department " + event.departmentId()));
    }
}
