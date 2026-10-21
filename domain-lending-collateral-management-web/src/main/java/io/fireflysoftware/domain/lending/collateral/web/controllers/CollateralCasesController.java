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

package io.fireflysoftware.domain.lending.collateral.web.controllers;

import io.fireflysoftware.core.lending.collateral.sdk.model.CollateralCaseDTO;
import io.fireflysoftware.domain.lending.collateral.core.commands.CreateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseStatusCommand;
import io.fireflysoftware.domain.lending.collateral.core.services.CollateralCasesService;
import io.fireflysoftware.domain.lending.collateral.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.collateral.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for collateral cases.
 */
@RestController
@RequestMapping("/api/v1/collateral-cases")
@Tag(name = "CollateralCases", description = "Domain orchestration for collateral cases")
public class CollateralCasesController {

    private final CollateralCasesService service;

    public CollateralCasesController(CollateralCasesService service) {
        this.service = service;
    }

    @Operation(summary = "Create collateral case", description = "Runs the register saga against core-lending-collateral-management.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollateralCaseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollateralCaseDTO>> create(@RequestBody CollateralCaseDTO body) {
        return service.create(new CreateCollateralCaseCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get collateral case", description = "Reads one record from core-lending-collateral-management.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollateralCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{collateralCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollateralCaseDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collateralCaseId) {
        return service.get(collateralCaseId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-lending-collateral-management.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollateralCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{collateralCaseId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollateralCaseDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collateralCaseId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdateCollateralCaseStatusCommand(collateralCaseId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update collateral case", description = "Runs the update saga against core-lending-collateral-management. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollateralCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{collateralCaseId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollateralCaseDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collateralCaseId,
            @RequestBody CollateralCaseDTO body) {
        return service.update(new UpdateCollateralCaseCommand(collateralCaseId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Close collateral case", description = "Closes a collateral case by writing RELEASED when the collateral core allows that status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollateralCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{collateralCaseId}/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollateralCaseDTO>> close(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collateralCaseId,
            @RequestBody ReasonRequest request) {
        return service.close(collateralCaseId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove collateral case", description = "Deletes the record through core-lending-collateral-management when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{collateralCaseId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collateralCaseId) {
        return service.remove(collateralCaseId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
