package tw.elliot.cctest.department.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tw.elliot.cctest.department.domain.Department;
import tw.elliot.cctest.department.domain.DepartmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @Test
    void createShouldSaveDepartment() {
        departmentService = new DepartmentService(departmentRepository);
        UUID id = UUID.randomUUID();

        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department result = departmentService.create(id, "Test Dept", "Test Description");

        assertEquals("Test Dept", result.getName());
        assertEquals("Test Description", result.getDescription());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void updateShouldUpdateDepartment() {
        departmentService = new DepartmentService(departmentRepository);
        UUID id = UUID.randomUUID();
        Department dept = new Department(id, "Old Name", "Old Desc");

        when(departmentRepository.findById(id)).thenReturn(Optional.of(dept));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department result = departmentService.update(id, "New Name", "New Desc");

        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());
    }

    @Test
    void getByIdShouldReturnDepartment() {
        departmentService = new DepartmentService(departmentRepository);
        UUID id = UUID.randomUUID();
        Department dept = new Department(id, "Test", "Desc");

        when(departmentRepository.findById(id)).thenReturn(Optional.of(dept));

        Department result = departmentService.getById(id);

        assertEquals("Test", result.getName());
    }

    @Test
    void getByIdShouldThrowExceptionForNonExistentDepartment() {
        departmentService = new DepartmentService(departmentRepository);
        UUID id = UUID.randomUUID();

        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> departmentService.getById(id));
    }

    @Test
    void getAllShouldReturnAllDepartments() {
        departmentService = new DepartmentService(departmentRepository);

        when(departmentRepository.findAll()).thenReturn(List.of(
            new Department(UUID.randomUUID(), "Dept1", "Desc1"),
            new Department(UUID.randomUUID(), "Dept2", "Desc2")
        ));

        List<Department> result = departmentService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void deleteShouldDeleteDepartment() {
        departmentService = new DepartmentService(departmentRepository);
        UUID id = UUID.randomUUID();

        doNothing().when(departmentRepository).deleteById(id);

        departmentService.delete(id);

        verify(departmentRepository).deleteById(id);
    }
}
