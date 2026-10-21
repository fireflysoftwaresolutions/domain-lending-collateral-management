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

package io.fireflysoftware.domain.lending.collateral.core.handlers;

import io.fireflysoftware.core.lending.collateral.sdk.api.CollateralCaseApi;
import io.fireflysoftware.core.lending.collateral.sdk.model.CollateralCaseDTO;
import io.fireflysoftware.domain.lending.collateral.core.commands.CreateCollateralCaseCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Creates a collateral case by calling {@code core-lending-collateral-management}.
 */
@Slf4j
@CommandHandlerComponent
public class CreateCollateralCaseHandler extends CommandHandler<CreateCollateralCaseCommand, CollateralCaseDTO> {

    private final CollateralCaseApi api;

    public CreateCollateralCaseHandler(CollateralCaseApi api) {
        this.api = api;
    }

    @Override
    protected Mono<CollateralCaseDTO> doHandle(CreateCollateralCaseCommand command) {
        log.debug("Creating collateral case");
        return api.create5(command.record(), UUID.randomUUID().toString());
    }
}
