/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.lending.collateral.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Entry point for domain-lending-collateral-management.
 */
@SpringBootApplication(scanBasePackages = {
        "io.fireflysoftware.domain.lending.collateral",
        "org.fireflyframework.web"
})
@EnableWebFlux
@ConfigurationPropertiesScan(basePackages = "io.fireflysoftware.domain.lending.collateral")
@OpenAPIDefinition(
info = @Info(
        title = "Domain Lending Collateral Management API",
        version = "1.0.0",
        description = "Orchestrates collateral cases through core-lending-collateral-management. It creates, updates, changes status, and closes collateral cases through the collateral core. Party, contract, and product identifiers stay UUIDs.",
        contact = @Contact(
                name = "Firefly Software Foundation",
                email = "dev@fireflysoftware.io"
        )
),
servers = {
        @Server(
                url = "https://openplatform.fireflysoftware.io/domain-lending-collateral-management",
                description = "Development Environment"
        ),
        @Server(
                url = "/",
                description = "Local Development Environment"
        )
}
)

public class DomainLendingCollateralManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomainLendingCollateralManagementApplication.class, args);
    }
}
