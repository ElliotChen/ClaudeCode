package tw.elliot.cctest.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import tw.elliot.cctest.config.UuidV7;

import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    public Department(String name, String description) {
        this.id = UuidV7.generate();
        this.name = name;
        this.description = description;
    }
}
