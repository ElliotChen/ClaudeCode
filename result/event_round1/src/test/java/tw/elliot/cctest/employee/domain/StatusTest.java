package tw.elliot.cctest.employee.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusTest {

    @Test
    void shouldHaveThreeStatuses() {
        assertEquals(3, Status.values().length);
    }

    @Test
    void statusNamesShouldBeCorrect() {
        assertEquals("ACTIVE", Status.ACTIVE.name());
        assertEquals("TERMINATED", Status.TERMINATED.name());
        assertEquals("SUSPENDED", Status.SUSPENDED.name());
    }
}
