package tw.elliot.cctest.employee.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RankTest {

    @Test
    void rankOrderShouldBeCorrect() {
        assertEquals(0, Rank.JUNIOR.ordinal());
        assertEquals(1, Rank.MID.ordinal());
        assertEquals(2, Rank.SENIOR.ordinal());
        assertEquals(3, Rank.PRINCIPAL.ordinal());
        assertEquals(4, Rank.MANAGER.ordinal());
    }

    @Test
    void shouldHaveFiveRanks() {
        assertEquals(5, Rank.values().length);
    }
}
