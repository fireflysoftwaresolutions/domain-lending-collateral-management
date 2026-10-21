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
import io.fireflysoftware.domain.lending.collateral.core.commands.UpdateCollateralCaseCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateCollateralCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateCollateralCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateCollateralCaseSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        CollateralCaseDTO current = new CollateralCaseDTO(id);
        current.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.PENDING);
        CollateralCaseDTO incoming = new CollateralCaseDTO();
        incoming.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyCollateralCaseStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyCollateralCaseStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(CollateralCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(CollateralCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateCollateralCase(new UpdateCollateralCaseCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getCollateralStatus()).isEqualTo(CollateralCaseDTO.CollateralStatusEnum.PENDING))
                .verifyComplete();

        ArgumentCaptor<ApplyCollateralCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollateralCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collateralCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getCollateralStatus()).isEqualTo(CollateralCaseDTO.CollateralStatusEnum.PENDING);
        assertThat(ctx.getVariableAs(CollateralCaseSagaConstants.CTX_PREVIOUS_RECORD, CollateralCaseDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        CollateralCaseDTO previous = new CollateralCaseDTO(id);
        previous.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.PENDING);
        CollateralCaseDTO updated = new CollateralCaseDTO(id);
        updated.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyCollateralCaseStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(CollateralCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(CollateralCaseSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreCollateralCase(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyCollateralCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollateralCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
