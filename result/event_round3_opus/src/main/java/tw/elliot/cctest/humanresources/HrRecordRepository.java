package tw.elliot.cctest.humanresources;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HrRecordRepository extends JpaRepository<HrRecord, UUID> {

    List<HrRecord> findByEmployeeId(UUID employeeId);
}
