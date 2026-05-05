package tw.elliot.cctest.humanresources;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import tw.elliot.cctest.config.UuidV7;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hr_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HrRecord {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public HrRecord(UUID employeeId, ActionType actionType, String detail) {
        this.id = UuidV7.generate();
        this.employeeId = employeeId;
        this.actionType = actionType;
        this.detail = detail;
        this.occurredAt = LocalDateTime.now();
    }
}
