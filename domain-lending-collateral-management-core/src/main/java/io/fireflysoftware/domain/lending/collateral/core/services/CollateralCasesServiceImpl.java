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
import io.fireflysoftware.domain.lending.collateral.core.commands.GetCollateralCaseQuery;
import io.fireflysoftware.domain.lending.collateral.core.commands.RemoveCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseStatusCommand;
import io.fireflysoftware.domain.lending.collateral.core.lifecycle.CollateralCaseLifecycle;
import io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the collateral case sagas and reads through the query bus.
 */
@Service
public class CollateralCasesServiceImpl implements CollateralCasesService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public CollateralCasesServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<CollateralCaseDTO> create(CreateCollateralCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_REGISTER, CollateralCaseDTO.class));
    }

    @Override
    public Mono<CollateralCaseDTO> updateStatus(UpdateCollateralCaseStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_RETRIEVE, new GetCollateralCaseQuery(command.collateralCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_UPDATE_STATUS, CollateralCaseDTO.class));
    }

    @Override
    public Mono<CollateralCaseDTO> get(UUID collateralCaseId) {
        return queryBus.query(new GetCollateralCaseQuery(collateralCaseId));
    }

    @Override
    public Mono<CollateralCaseDTO> update(UpdateCollateralCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetCollateralCaseQuery(command.collateralCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_UPDATE_RECORD, CollateralCaseDTO.class));
    }

    @Override
    public Mono<CollateralCaseDTO> close(UUID collateralCaseId, String statusReason) {
        return transition(collateralCaseId, CollateralCaseDTO.CollateralStatusEnum.RELEASED, statusReason);
    }

    private Mono<CollateralCaseDTO> transition(UUID collateralCaseId, CollateralCaseDTO.CollateralStatusEnum target, String statusReason) {
        return get(collateralCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Collateral case was not found")))
                .flatMap(current -> {
                    if (!CollateralCaseLifecycle.canTransition(current.getCollateralStatus(), target)) {
                        String from = current.getCollateralStatus() == null
                                ? "unset"
                                : current.getCollateralStatus().getValue();
                        return Mono.<CollateralCaseDTO>error(new IllegalStateException(
                                "Collateral case " + collateralCaseId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateCollateralCaseStatusCommand(collateralCaseId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID collateralCaseId) {
        return get(collateralCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Collateral case was not found")))
                .flatMap(current -> {
                    if (!CollateralCaseLifecycle.canRemove(current.getCollateralStatus())) {
                        String statusValue = current.getCollateralStatus() == null
                                ? "unset"
                                : current.getCollateralStatus().getValue();
                        return Mono.error(new IllegalStateException(
                                "Collateral case " + collateralCaseId + " is " + statusValue
                                        + " and cannot be deleted; close it so the case is kept"));
                    }
                    return commandBus.send(new RemoveCollateralCaseCommand(collateralCaseId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
