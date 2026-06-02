package com.gui.particles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "jwt.issuer-uri=https://accounts.google.com",
        "github.clientId=test-client",
        "github.secret=test-secret",
        "customJwt.jwksUri=http://localhost:9090/.well-known/jwks.json"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}
