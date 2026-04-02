package tw.elliot.cctest.employee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Rank {
    JUNIOR(1),
    MID(2),
    SENIOR(3),
    LEAD(4),
    PRINCIPAL(5);

    private final int level;

    public static Rank fromLevel(int level) {
        for (Rank rank : values()) {
            if (rank.level == level) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Invalid rank level: " + level);
    }

    public Rank promote() {
        int nextLevel = this.level + 1;
        for (Rank rank : values()) {
            if (rank.level == nextLevel) {
                return rank;
            }
        }
        return this;
    }

    public Rank demote() {
        int prevLevel = this.level - 1;
        for (Rank rank : values()) {
            if (rank.level == prevLevel) {
                return rank;
            }
        }
        return this;
    }
}