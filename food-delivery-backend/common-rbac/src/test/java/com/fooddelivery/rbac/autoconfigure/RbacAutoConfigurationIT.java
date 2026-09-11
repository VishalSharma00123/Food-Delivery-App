package com.fooddelivery.rbac.autoconfigure;

import com.fooddelivery.rbac.JwtService;
import com.fooddelivery.rbac.it.MinimalSpringBootApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = MinimalSpringBootApp.class,
        properties = {
                "jwt.secret=01234567890123456789012345678901",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
        }
)
class RbacAutoConfigurationIT {

    @Autowired
    private JwtService jwtService;

    @Test
    void autoConfigurationRegistersJwtService() {
        assertNotNull(jwtService);
    }
}
