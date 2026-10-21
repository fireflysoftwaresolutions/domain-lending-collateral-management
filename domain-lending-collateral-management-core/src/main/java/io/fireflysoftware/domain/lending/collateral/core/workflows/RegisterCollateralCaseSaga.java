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
import io.fireflysoftware.domain.lending.collateral.core.commands.CreateCollateralCaseCommand;
import io.fireflysoftware.domain.lending.collateral.core.commands.RemoveCollateralCaseCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.COMPENSATE_REMOVE;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.EVENT_REGISTERED;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.SAGA_REGISTER;
import static io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants.STEP_REGISTER;




/**
 * Creates a collateral case on {@code core-lending-collateral-management} and compensates by deleting it.
 */
@Saga(name = SAGA_REGISTER)
@Service
public class RegisterCollateralCaseSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterCollateralCaseSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER, compensate = COMPENSATE_REMOVE)
    @StepEvent(type = EVENT_REGISTERED)
    public Mono<CollateralCaseDTO> registerCollateralCase(CreateCollateralCaseCommand command, ExecutionContext ctx) {
        return commandBus.send(command)
                .doOnNext(created -> ctx.putVariable(CTX_RECORD, created));
    }

    public Mono<Void> removeCollateralCase(CollateralCaseDTO created) {
        if (created == null || created.getCollateralCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveCollateralCaseCommand(created.getCollateralCaseId()));
    }

}
