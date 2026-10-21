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

package io.fireflysoftware.domain.lending.collateral.core.services;

import io.fireflysoftware.core.lending.collateral.sdk.model.CollateralCaseDTO;
import io.fireflysoftware.domain.lending.collateral.core.commands.CreateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseStatusCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for collateral case orchestration.
 */
public interface CollateralCasesService {

    /**
     * Creates a collateral case through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<CollateralCaseDTO> create(CreateCollateralCaseCommand command);

    /**
     * Moves a collateral case to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<CollateralCaseDTO> updateStatus(UpdateCollateralCaseStatusCommand command);

    /**
     * Reads one collateral case.
     *
     * @param collateralCaseId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<CollateralCaseDTO> get(UUID collateralCaseId);

    /**
     * Updates fields on a collateral case and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<CollateralCaseDTO> update(UpdateCollateralCaseCommand command);

    /**
     * Close collateral case when the core allows RELEASED.
     *
     * @param collateralCaseId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<CollateralCaseDTO> close(UUID collateralCaseId, String statusReason);

    /**
     * Deletes a collateral case when the core lifecycle allows removal.
     *
     * @param collateralCaseId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID collateralCaseId);
}
