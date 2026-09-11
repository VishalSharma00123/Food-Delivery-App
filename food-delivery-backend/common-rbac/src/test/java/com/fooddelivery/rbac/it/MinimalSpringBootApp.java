package com.fooddelivery.rbac.it;

import com.fooddelivery.rbac.autoconfigure.RbacAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RbacAutoConfiguration.class)
public class MinimalSpringBootApp {
}
