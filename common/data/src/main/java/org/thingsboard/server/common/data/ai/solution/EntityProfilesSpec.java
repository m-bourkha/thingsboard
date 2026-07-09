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
import java.util.List;

/**
 * The "Entity profiles" section of an {@link AiSolutionSpec}: device profiles, asset profiles
 * and customers, each with the sample instances and telemetry keys used by alarms, calculated
 * fields and the telemetry generator.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntityProfilesSpec(
        @Schema(description = "Device profiles to create, each with sample devices")
        List<DeviceProfileSpec> deviceProfiles,

        @Schema(description = "Asset profiles to create, each with sample assets")
        List<AssetProfileSpec> assetProfiles,

        @Schema(description = "Customers to create")
        List<CustomerSpec> customers
) implements Serializable {

    public List<DeviceProfileSpec> deviceProfilesOrEmpty() {
        return deviceProfiles != null ? deviceProfiles : List.of();
    }

    public List<AssetProfileSpec> assetProfilesOrEmpty() {
        return assetProfiles != null ? assetProfiles : List.of();
    }

    public List<CustomerSpec> customersOrEmpty() {
        return customers != null ? customers : List.of();
    }

    /**
     * A device profile plus the sample devices and telemetry keys that describe it.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeviceProfileSpec(
            @Schema(description = "Profile name", example = "MagneticSensor")
            String name,

            @Schema(description = "Description of the device profile")
            String description,

            @Schema(description = "Name of the customer that owns devices of this profile (optional)")
            String owner,

            @Schema(description = "Number of sample devices to create for this profile", example = "5")
            Integer sampleCount,

            @Schema(description = "Telemetry and attribute keys reported by devices of this profile")
            List<TelemetryKeySpec> telemetry
    ) implements Serializable {

        public int sampleCountOrDefault() {
            return sampleCount != null && sampleCount > 0 ? Math.min(sampleCount, 50) : 3;
        }

        public List<TelemetryKeySpec> telemetryOrEmpty() {
            return telemetry != null ? telemetry : List.of();
        }
    }

    /**
     * An asset profile plus the sample assets that describe it.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssetProfileSpec(
            @Schema(description = "Profile name", example = "ParkingLot")
            String name,

            @Schema(description = "Description of the asset profile")
            String description,

            @Schema(description = "Name of the customer that owns assets of this profile (optional)")
            String owner,

            @Schema(description = "Number of sample assets to create for this profile", example = "2")
            Integer sampleCount,

            @Schema(description = "Attribute keys stored on assets of this profile")
            List<TelemetryKeySpec> telemetry
    ) implements Serializable {

        public int sampleCountOrDefault() {
            return sampleCount != null && sampleCount > 0 ? Math.min(sampleCount, 50) : 2;
        }

        public List<TelemetryKeySpec> telemetryOrEmpty() {
            return telemetry != null ? telemetry : List.of();
        }
    }

    /**
     * A customer to create, optionally with server-scope attributes (e.g. thresholds).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerSpec(
            @Schema(description = "Customer title", example = "ParkingOperator")
            String name,

            @Schema(description = "Server-scope attributes to set on the customer (name -> value)")
            List<TelemetryKeySpec> attributes
    ) implements Serializable {

        public List<TelemetryKeySpec> attributesOrEmpty() {
            return attributes != null ? attributes : List.of();
        }
    }

}
