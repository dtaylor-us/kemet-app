package com.kemet.core.architecture;

import com.kemet.core.companion.CompanionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:adl025;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://example.invalid/"
})
class Adl025EndpointAuthenticationTest {

    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CompanionService companionService;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void adl025_everyCoreEndpointRequiresAuthenticationExceptHealth() throws Exception {
        for (var entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            Class<?> beanType = entry.getValue().getBeanType();

            if (!beanType.getPackageName().startsWith("com.kemet.core") || !beanType.isAnnotationPresent(RestController.class)) {
                continue;
            }

            Set<HttpMethod> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(requestMethod -> HttpMethod.valueOf(requestMethod.name()))
                    .collect(java.util.stream.Collectors.toSet());
            if (methods.isEmpty()) {
                methods = Set.of(HttpMethod.GET);
            }

            for (String rawPattern : mappingInfo.getPatternValues()) {
                String path = rawPattern.replaceAll("\\{[^/]+\\}", "sample");
                for (HttpMethod method : methods) {
                    MockHttpServletRequestBuilder requestBuilder = request(method, path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}");

                    int actualStatus = mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus();
                    assertThat(actualStatus)
                            .withFailMessage("ADL-025 violated: endpoint %s %s should return 401 without Authorization header but returned %s",
                                    method, path, actualStatus)
                            .isEqualTo(401);
                }
            }
        }

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
