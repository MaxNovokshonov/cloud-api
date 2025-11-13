package ru.netology.cloud_api.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.repository.UserRepository;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class CloudApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("server.servlet.context-path", () -> "/cloud");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureTestUser() {
        userRepository.findByUsername("user1").orElseGet(() -> {
            User u = new User();
            u.setUsername("user1");
            u.setPasswordHash(passwordEncoder.encode("pass"));
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("E2E: login → upload → list → download → rename → delete → logout → 401")
    void endToEndFlow() throws Exception {
        // LOGIN
        String loginJson = "{\"login\":\"user1\",\"password\":\"pass\"}";
        MvcResult loginRes = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['auth-token']", not(isEmptyOrNullString())))
                .andReturn();

        String token = om.readTree(loginRes.getResponse().getContentAsString())
                .path("auth-token").asText();

        // UPLOAD
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello world".getBytes()
        );
        mvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "test.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // LIST
        mvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("test.txt")));

        // DOWNLOAD
        MvcResult dl = mvc.perform(get("/file")
                        .param("filename", "test.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.startsWith("multipart/form-data")))
                .andReturn();
        String body = dl.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).contains("hello world");

        // RENAME
        mvc.perform(put("/file")
                        .param("filename", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed.txt\"}")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // DELETE
        mvc.perform(delete("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // LOGOUT
        mvc.perform(post("/logout")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // После логаута — 401 на любые защищённые методы
        mvc.perform(get("/list")
                        .param("limit", "3")
                        .header("auth-token", token))
                .andExpect(status().isUnauthorized());
    }
}
