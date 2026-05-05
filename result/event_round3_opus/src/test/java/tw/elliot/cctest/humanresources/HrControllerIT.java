package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.department.DepartmentService;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class HrControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentService departmentService;

    @Test
    void hireEmployee_shouldReturn201() throws Exception {
        var dept = departmentService.create("HR-hire-dept", "For hire test");

        mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "email": "alice-hire@test.com",
                                  "phone": "0912345678",
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.rank").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.departmentId").value(dept.getId().toString()));
    }

    @Test
    void fireEmployee_shouldSetInactive() throws Exception {
        var dept = departmentService.create("HR-fire-dept", "For fire test");

        // Hire first
        var hireResult = mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bob",
                                  "email": "bob-fire@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = com.jayway.jsonpath.JsonPath.read(
                hireResult.getResponse().getContentAsString(), "$.id");

        // Fire
        mockMvc.perform(post("/api/hr/employees/{id}/fire", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.departmentId").isEmpty());
    }

    @Test
    void listRecords_shouldReturnHireRecord() throws Exception {
        var dept = departmentService.create("HR-records-dept", "For records test");

        // Hire to create a record
        mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Carol",
                                  "email": "carol-records@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated());

        // List records
        mockMvc.perform(get("/api/hr/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listRecordsByEmployeeId_shouldFilterCorrectly() throws Exception {
        var dept = departmentService.create("HR-filter-dept", "For filter test");

        var hireResult = mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dave",
                                  "email": "dave-filter@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = com.jayway.jsonpath.JsonPath.read(
                hireResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/hr/records").param("employeeId", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actionType").value("HIRED"));
    }
}
