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
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseStatusCommand;
import io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateCollateralCaseStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateCollateralCaseStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateCollateralCaseStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        CollateralCaseDTO current = new CollateralCaseDTO(id);
        current.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.PENDING);
        when(commandBus.send(any(ApplyCollateralCaseStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(CollateralCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(CollateralCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateCollateralCaseStatus(new UpdateCollateralCaseStatusCommand(id, "ACTIVE", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyCollateralCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollateralCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collateralCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getCollateralStatus()).isEqualTo(CollateralCaseDTO.CollateralStatusEnum.ACTIVE);
        assertThat(ctx.getVariableAs(CollateralCaseSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("PENDING");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        CollateralCaseDTO updated = new CollateralCaseDTO(id);
        updated.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyCollateralCaseStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(CollateralCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(CollateralCaseSagaConstants.CTX_PREVIOUS_STATUS, "PENDING");

        StepVerifier.create(saga.restoreCollateralCaseStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyCollateralCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollateralCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getCollateralStatus()).isEqualTo(CollateralCaseDTO.CollateralStatusEnum.PENDING);
    }

}
