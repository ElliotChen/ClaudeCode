package tw.elliot.cctest.department.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.department.domain.Department;
import tw.elliot.cctest.department.domain.DepartmentRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public Department create(UUID id, String name, String description) {
        Department department = new Department(id, name, description);
        return departmentRepository.save(department);
    }

    public Department update(UUID id, String name, String description) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        if (name != null) {
            department.updateName(name);
        }
        if (description != null) {
            department.updateDescription(description);
        }
        return departmentRepository.save(department);
    }

    public Department getById(UUID id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    public void delete(UUID id) {
        departmentRepository.deleteById(id);
    }
}
