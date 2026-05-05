package tw.elliot.cctest.employee;

public enum Rank {
    STAFF,
    TEAM_LEAD,
    MANAGER;

    public Rank promote() {
        return switch (this) {
            case STAFF -> TEAM_LEAD;
            case TEAM_LEAD -> MANAGER;
            case MANAGER -> throw new IllegalStateException("Cannot promote beyond MANAGER");
        };
    }

    public Rank demote() {
        return switch (this) {
            case MANAGER -> TEAM_LEAD;
            case TEAM_LEAD -> STAFF;
            case STAFF -> throw new IllegalStateException("Cannot demote below STAFF");
        };
    }
}
