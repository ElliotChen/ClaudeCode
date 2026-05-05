package tw.elliot.cctest.humanresources.api;

import tw.elliot.cctest.humanresources.ActionType;
import tw.elliot.cctest.humanresources.HrRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record HrRecordDto(
        UUID id,
        UUID employeeId,
        ActionType actionType,
        String detail,
        LocalDateTime occurredAt
) {
    public static HrRecordDto from(HrRecord record) {
        return new HrRecordDto(
                record.getId(),
                record.getEmployeeId(),
                record.getActionType(),
                record.getDetail(),
                record.getOccurredAt()
        );
    }
}
