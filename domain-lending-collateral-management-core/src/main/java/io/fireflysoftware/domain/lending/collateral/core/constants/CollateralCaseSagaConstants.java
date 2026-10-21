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

package io.fireflysoftware.domain.lending.collateral.core.constants;

/**
 * Names shared by the collateral case sagas, their steps, and the saga engine.
 */
public final class CollateralCaseSagaConstants {

    public static final String SAGA_REGISTER = "RegisterCollateralCaseSaga";
    public static final String SAGA_UPDATE_STATUS = "UpdateCollateralCaseStatusSaga";
    public static final String SAGA_UPDATE_RECORD = "UpdateCollateralCaseSaga";

    public static final String STEP_REGISTER = "registerCollateralCase";
    public static final String STEP_RETRIEVE = "retrieveCollateralCase";
    public static final String STEP_UPDATE_STATUS = "updateCollateralCaseStatus";
    public static final String STEP_RETRIEVE_FOR_UPDATE = "retrieveCollateralCaseForUpdate";
    public static final String STEP_UPDATE_RECORD = "updateCollateralCase";

    public static final String COMPENSATE_REMOVE = "removeCollateralCase";
    public static final String COMPENSATE_RESTORE = "restoreCollateralCaseStatus";
    public static final String COMPENSATE_RESTORE_RECORD = "restoreCollateralCase";

    public static final String EVENT_REGISTERED = "collateral-cases.registered";
    public static final String EVENT_RETRIEVED = "collateral-cases.retrieved";
    public static final String EVENT_STATUS_UPDATED = "collateral-cases.status.updated";
    public static final String EVENT_RECORD_UPDATED = "collateral-cases.updated";

    public static final String CTX_RECORD = "collateral-casesRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousCollateralCaseStatus";
    public static final String CTX_PREVIOUS_RECORD = "previousCollateralCaseRecord";

    private CollateralCaseSagaConstants() {
    }
}
