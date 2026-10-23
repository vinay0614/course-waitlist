package com.vinay.aiassistance.courseregistration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentApiTests {
    @Autowired MockMvc mvc;

    @Test void blankLearnerIdIsAValidationError() throws Exception {
        mvc.perform(post("/offerings/JAVA-ADV-2026-SEM1/enroll")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"learnerId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test void malformedJsonIsAClientError() throws Exception {
        mvc.perform(post("/offerings/JAVA-ADV-2026-SEM1/enroll")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test void operationResponsesStayMinimalAndGetShowsPostDropPromotionState() throws Exception {
        postEnrollment("L101", "ENROLLED");
        postEnrollment("L104", "ENROLLED");
        postEnrollment("L105", "WAITLISTED");

        mvc.perform(get("/offerings/JAVA-ADV-2026-SEM1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offeringId").value("JAVA-ADV-2026-SEM1"))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.enrolledLearnerIds").isArray())
                .andExpect(jsonPath("$.enrolledLearnerIds.length()").value(2))
                .andExpect(jsonPath("$.availableSeats").value(0))
                .andExpect(jsonPath("$.waitlist[0]").value("L105"));

        mvc.perform(delete("/offerings/JAVA-ADV-2026-SEM1/enrollments/L101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DROPPED"))
                .andExpect(jsonPath("$.message").value("Enrollment dropped"));

        mvc.perform(get("/offerings/JAVA-ADV-2026-SEM1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledLearnerIds[0]").value("L104"))
                .andExpect(jsonPath("$.enrolledLearnerIds[1]").value("L105"))
                .andExpect(jsonPath("$.availableSeats").value(0))
                .andExpect(jsonPath("$.waitlist").isEmpty());
    }

    private void postEnrollment(String learnerId, String outcome) throws Exception {
        mvc.perform(post("/offerings/JAVA-ADV-2026-SEM1/enroll")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"learnerId\":\"" + learnerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value(outcome))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
