package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfig.class)
class EventDeliveryIT {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private HrRecordRepository hrRecordRepository;

    @Test
    void promote_shouldCreateHrRecord() {
        var dept = departmentService.create("Event-promo-dept", "For event test");
        var employee = employeeService.create("EventPromo", "eventpromo@test.com", null, dept.getId());

        departmentService.promote(dept.getId(), employee.getId());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var records = hrRecordRepository.findByEmployeeId(employee.getId());
            assertThat(records).anyMatch(r ->
                    r.getActionType() == ActionType.PROMOTED
                            && r.getDetail().contains(Rank.STAFF.name())
                            && r.getDetail().contains(Rank.TEAM_LEAD.name()));
        });
    }

    @Test
    void transfer_shouldCreateHrRecord() {
        var deptA = departmentService.create("Event-deptA", "Dept A");
        var deptB = departmentService.create("Event-deptB", "Dept B");
        var employee = employeeService.create("EventTransfer", "eventtransfer@test.com", null, deptA.getId());

        departmentService.transfer(deptB.getId(), employee.getId());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var records = hrRecordRepository.findByEmployeeId(employee.getId());
            assertThat(records).anyMatch(r ->
                    r.getActionType() == ActionType.TRANSFERRED
                            && r.getDetail().contains(deptA.getId().toString())
                            && r.getDetail().contains(deptB.getId().toString()));
        });
    }

    @Test
    void demote_shouldCreateHrRecord() {
        var dept = departmentService.create("Event-demote-dept", "For demote event test");
        var employee = employeeService.create("EventDemote", "eventdemote@test.com", null, dept.getId());
        employeeService.promote(employee.getId()); // STAFF -> TEAM_LEAD

        departmentService.demote(dept.getId(), employee.getId());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var records = hrRecordRepository.findByEmployeeId(employee.getId());
            assertThat(records).anyMatch(r ->
                    r.getActionType() == ActionType.DEMOTED
                            && r.getDetail().contains(Rank.TEAM_LEAD.name())
                            && r.getDetail().contains(Rank.STAFF.name()));
        });
    }
}
