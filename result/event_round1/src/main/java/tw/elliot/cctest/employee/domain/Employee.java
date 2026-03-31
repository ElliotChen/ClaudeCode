package tw.elliot.cctest.employee.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rank rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {
    }

    public Employee(UUID id, String name, UUID departmentId, Rank rank) {
        this.id = id;
        this.name = name;
        this.departmentId = departmentId;
        this.rank = rank;
        this.status = Status.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getDepartmentId() { return departmentId; }
    public Rank getRank() { return rank; }
    public Status getStatus() { return status; }

    public void terminate() {
        this.status = Status.TERMINATED;
    }

    public void transfer(UUID newDepartmentId) {
        this.departmentId = newDepartmentId;
    }

    public void promote() {
        int currentOrdinal = this.rank.ordinal();
        if (currentOrdinal < Rank.values().length - 1) {
            this.rank = Rank.values()[currentOrdinal + 1];
        }
    }

    public void demote() {
        int currentOrdinal = this.rank.ordinal();
        if (currentOrdinal > 0) {
            this.rank = Rank.values()[currentOrdinal - 1];
        }
    }
}
