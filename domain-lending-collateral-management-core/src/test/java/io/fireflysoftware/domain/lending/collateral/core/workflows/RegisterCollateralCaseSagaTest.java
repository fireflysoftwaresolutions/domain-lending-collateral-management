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
import io.fireflysoftware.domain.lending.collateral.core.constants.CollateralCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterCollateralCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterCollateralCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterCollateralCaseSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        CollateralCaseDTO created = new CollateralCaseDTO(id);
        created.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.PENDING);
        when(commandBus.send(any(CreateCollateralCaseCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(CollateralCaseSagaConstants.SAGA_REGISTER, "test");
        CollateralCaseDTO request = new CollateralCaseDTO();
        request.setCollateralStatus(CollateralCaseDTO.CollateralStatusEnum.PENDING);

        StepVerifier.create(saga.registerCollateralCase(new CreateCollateralCaseCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(CollateralCaseSagaConstants.CTX_RECORD, CollateralCaseDTO.class).getCollateralCaseId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveCollateralCaseCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeCollateralCase(new CollateralCaseDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemoveCollateralCaseCommand> captor = ArgumentCaptor.forClass(RemoveCollateralCaseCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collateralCaseId()).isEqualTo(id);
    }

}
