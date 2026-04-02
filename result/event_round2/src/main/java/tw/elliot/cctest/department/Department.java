package tw.elliot.cctest.department;

import jakarta.persistence.*;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Version
    private Long version;

    protected Department() {
    }

    @Builder
    public Department(String name, String code) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Long getVersion() {
        return version;
    }
}
