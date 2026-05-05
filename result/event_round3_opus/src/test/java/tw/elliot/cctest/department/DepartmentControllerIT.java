package tw.elliot.cctest.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.employee.EmployeeService;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DepartmentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Test
    void createDepartment_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Engineering", "description": "Engineering department"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void listDepartments_shouldReturnAll() throws Exception {
        departmentService.create("Sales-list", "Sales dept");

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void promoteEmployee_shouldReturnUpdatedRank() throws Exception {
        var dept = departmentService.create("Promo-dept", "For promotion test");
        var employee = employeeService.create("PromoTest", "promo@test.com", null, dept.getId());

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/promote",
                        dept.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("TEAM_LEAD"));
    }

    @Test
    void demoteEmployee_shouldReturnUpdatedRank() throws Exception {
        var dept = departmentService.create("Demote-dept", "For demotion test");
        var employee = employeeService.create("DemoteTest", "demote@test.com", null, dept.getId());
        employeeService.promote(employee.getId()); // STAFF -> TEAM_LEAD

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/demote",
                        dept.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("STAFF"));
    }

    @Test
    void transferEmployee_shouldChangeDepartment() throws Exception {
        var deptA = departmentService.create("DeptA-transfer", "Dept A");
        var deptB = departmentService.create("DeptB-transfer", "Dept B");
        var employee = employeeService.create("TransferTest", "transfer@test.com", null, deptA.getId());

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/transfer",
                        deptB.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(deptB.getId().toString()));
    }

    @Test
    void listEmployeesInDepartment_shouldReturnEmployees() throws Exception {
        var dept = departmentService.create("ListEmp-dept", "For listing");
        employeeService.create("EmpInDept", "empindept@test.com", null, dept.getId());

        mockMvc.perform(get("/api/departments/{id}/employees", dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("EmpInDept"));
    }
}
