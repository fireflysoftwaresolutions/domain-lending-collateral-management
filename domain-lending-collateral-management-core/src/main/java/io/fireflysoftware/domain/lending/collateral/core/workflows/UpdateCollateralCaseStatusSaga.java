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

package io.fireflysoftware.domain.lending.collateral.core.workflows;

import io.fireflysoftware.core.lending.collateral.sdk.model.CollateralCaseDTO;
import io.fireflysoftware.domain.lending.collateral.core.commands.ApplyCollateralCaseStatusCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.GetCollateralCaseQuery;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseStatusCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.COMPENSATE_RESTORE;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.SAGA_UPDATE_STATUS;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_RETRIEVE;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_UPDATE_STATUS;




/**
 * Changes a collateral case status by reading it and writing the new status through the core.
 */
@Saga(name = SAGA_UPDATE_STATUS)
@Service
public class UpdateCollateralCaseStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateCollateralCaseStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<CollateralCaseDTO> retrieveCollateralCase(GetCollateralCaseQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_STATUS, dependsOn = STEP_RETRIEVE, compensate = COMPENSATE_RESTORE)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<CollateralCaseDTO> updateCollateralCaseStatus(UpdateCollateralCaseStatusCommand command, ExecutionContext ctx) {
        CollateralCaseDTO current = ctx.getVariableAs(CTX_RECORD, CollateralCaseDTO.class);
        if (current == null || current.getCollateralCaseId() == null) {
            return Mono.error(new IllegalStateException("Collateral case was not found"));
        }
        if (current.getCollateralStatus() == null) {
            return Mono.error(new IllegalStateException("Collateral case has no current status"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        ctx.putVariable(CTX_PREVIOUS_STATUS, current.getCollateralStatus().getValue());
        current.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.fromValue(command.status()));
        return commandBus.send(new ApplyCollateralCaseStatusCommand(current.getCollateralCaseId(), current))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreCollateralCaseStatus(CollateralCaseDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getCollateralCaseId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous == null || previous.isBlank()) {
            return Mono.empty();
        }
        updated.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.fromValue(previous));
        return commandBus.send(new ApplyCollateralCaseStatusCommand(updated.getCollateralCaseId(), updated)).then();
    }

}
