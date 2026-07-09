/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.ai.solution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * An alarm rule to configure on a device profile. Modelled as a single threshold condition on a
 * telemetry key, which the installer translates into a {@code DeviceProfileAlarm} create-rule.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlarmSpec(
        @Schema(description = "Alarm type", example = "LowBattery")
        String type,

        @Schema(description = "Name of the device profile this alarm is configured on")
        String entityProfile,

        @Schema(description = "Alarm severity: CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE")
        String severity,

        @Schema(description = "Telemetry key evaluated by the condition", example = "battery")
        String key,

        @Schema(description = "Comparison operator: GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL, EQUAL, NOT_EQUAL")
        Operation operation,

        @Schema(description = "Threshold the key value is compared against", example = "15")
        Double threshold
) implements Serializable {

    public enum Operation {
        GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL, EQUAL, NOT_EQUAL
    }

}
