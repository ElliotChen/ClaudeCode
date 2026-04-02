package tw.elliot.cctest.employee;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees", indexes = @Index(name = "idx_employees_email", columnList = "email"))
@Getter
@NoArgsConstructor
public class Employee {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rank rank = Rank.JUNIOR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "department_id")
    private UUID departmentId;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Employee(String name, String email, Rank rank, Status status, UUID departmentId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.rank = rank != null ? rank : Rank.JUNIOR;
        this.status = status != null ? status : Status.ACTIVE;
        this.departmentId = departmentId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void assignToDepartment(UUID departmentId) {
        this.departmentId = departmentId;
        this.updatedAt = Instant.now();
    }

    public void removeFromDepartment() {
        this.departmentId = null;
        this.updatedAt = Instant.now();
    }

    public void promote() {
        this.rank = this.rank.promote();
        this.updatedAt = Instant.now();
    }

    public void demote() {
        this.rank = this.rank.demote();
        this.updatedAt = Instant.now();
    }

    public void terminate() {
        this.status = Status.TERMINATED;
        this.departmentId = null;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = Status.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void setOnLeave() {
        this.status = Status.ON_LEAVE;
        this.updatedAt = Instant.now();
    }
}