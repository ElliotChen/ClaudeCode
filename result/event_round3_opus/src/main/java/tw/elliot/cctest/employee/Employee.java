package tw.elliot.cctest.employee;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import tw.elliot.cctest.config.UuidV7;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Rank rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Status status;

    @Setter
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    public Employee(String name, String email, String phone, UUID departmentId) {
        this.id = UuidV7.generate();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.rank = Rank.STAFF;
        this.status = Status.ACTIVE;
        this.departmentId = departmentId;
        this.hireDate = LocalDate.now();
    }
}
