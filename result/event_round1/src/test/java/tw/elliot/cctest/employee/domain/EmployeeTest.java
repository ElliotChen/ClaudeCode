package tw.elliot.cctest.employee.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    @Test
    void newEmployeeShouldHaveActiveStatus() {
        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", deptId, Rank.JUNIOR);

        assertEquals(Status.ACTIVE, employee.getStatus());
    }

    @Test
    void terminateShouldSetStatusToTerminated() {
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.JUNIOR);

        employee.terminate();

        assertEquals(Status.TERMINATED, employee.getStatus());
    }

    @Test
    void transferShouldChangeDepartmentId() {
        UUID id = UUID.randomUUID();
        UUID oldDeptId = UUID.randomUUID();
        UUID newDeptId = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", oldDeptId, Rank.JUNIOR);

        employee.transfer(newDeptId);

        assertEquals(newDeptId, employee.getDepartmentId());
    }

    @Test
    void promoteShouldIncreaseRank() {
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.JUNIOR);

        employee.promote();

        assertEquals(Rank.MID, employee.getRank());
    }

    @Test
    void demoteShouldDecreaseRank() {
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.MID);

        employee.demote();

        assertEquals(Rank.JUNIOR, employee.getRank());
    }

    @Test
    void juniorEmployeeCannotDemote() {
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.JUNIOR);

        employee.demote();

        assertEquals(Rank.JUNIOR, employee.getRank());
    }

    @Test
    void managerCannotPromote() {
        UUID id = UUID.randomUUID();
        Employee employee = new Employee(id, "Test", UUID.randomUUID(), Rank.MANAGER);

        employee.promote();

        assertEquals(Rank.MANAGER, employee.getRank());
    }
}
