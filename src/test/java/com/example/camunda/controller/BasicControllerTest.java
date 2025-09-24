package com.example.camunda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BasicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetEmployees() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetCompanies() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetConnectionStatus() throws Exception {
        mockMvc.perform(get("/api/connection-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.connected").exists());
    }

    @Test
    void testGetWorkerStatus() throws Exception {
        mockMvc.perform(get("/api/worker-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.['match-customer-with-dri']").exists())
                .andExpect(jsonPath("$.['query-for-company']").exists());
    }

    @Test
    void testGetJobHistory() throws Exception {
        mockMvc.perform(get("/api/job-history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetJobMetrics() throws Exception {
        mockMvc.perform(get("/api/job-metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalJobs").exists())
                .andExpect(jsonPath("$.jobsToday").exists())
                .andExpect(jsonPath("$.jobsByType").exists());
    }
}
