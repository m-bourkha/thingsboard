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
package org.thingsboard.server.service.ai.solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.solution.AiSolution;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.DeviceProfileSpec;
import org.thingsboard.server.common.data.ai.solution.InstalledEntity;
import org.thingsboard.server.common.data.ai.solution.TelemetryKeySpec;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.dao.ai.AiSolutionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emulates live device data for installed AI solutions. On a fixed schedule it walks every
 * {@link AiSolutionStatus#INSTALLED} solution and, for each created device, pushes a random in-range
 * sample for the numeric TIMESERIES keys declared on its device profile — so generated dashboards
 * and alarms/calculated fields have realistic moving data. There is no built-in emulator in the
 * platform, so this stands in for one.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class AiSolutionTelemetryGenerator {

    private final AiSolutionService aiSolutionService;
    private final TelemetrySubscriptionService telemetryService;

    @Value("${ai.solution.telemetry.enabled:true}")
    private boolean enabled;

    @Scheduled(
            initialDelayString = "${ai.solution.telemetry.initial-delay-ms:60000}",
            fixedDelayString = "${ai.solution.telemetry.generation-interval-ms:60000}")
    public void generate() {
        if (!enabled) {
            return;
        }
        List<AiSolution> installed;
        try {
            installed = aiSolutionService.findAllByStatus(AiSolutionStatus.INSTALLED);
        } catch (Exception e) {
            log.warn("Failed to load installed AI solutions for telemetry generation", e);
            return;
        }
        for (AiSolution solution : installed) {
            try {
                generateForSolution(solution);
            } catch (Exception e) {
                log.debug("Failed to generate telemetry for solution {}", solution.getId(), e);
            }
        }
    }

    private void generateForSolution(AiSolution solution) {
        if (solution.getSpec() == null || solution.getInstalledEntities() == null) {
            return;
        }
        TenantId tenantId = solution.getTenantId();
        List<DeviceProfileSpec> profiles = solution.getSpec().entityProfilesOrEmpty().deviceProfilesOrEmpty();
        long ts = System.currentTimeMillis();
        for (InstalledEntity entity : solution.getInstalledEntities()) {
            if (entity.entityType() != EntityType.DEVICE) {
                continue;
            }
            DeviceProfileSpec profile = profileForDevice(profiles, entity.name());
            if (profile == null) {
                continue;
            }
            List<KvEntry> values = sampleValues(profile.telemetryOrEmpty());
            if (values.isEmpty()) {
                continue;
            }
            DeviceId deviceId = new DeviceId(entity.id());
            for (KvEntry value : values) {
                telemetryService.saveTimeseries(TimeseriesSaveRequest.builder()
                        .tenantId(tenantId)
                        .entityId(deviceId)
                        .entry(new BasicTsKvEntry(ts, value))
                        .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                        .build());
            }
        }
    }

    private static DeviceProfileSpec profileForDevice(List<DeviceProfileSpec> profiles, String deviceName) {
        if (deviceName == null) {
            return null;
        }
        for (DeviceProfileSpec profile : profiles) {
            if (profile.name() != null && deviceName.startsWith(profile.name() + " ")) {
                return profile;
            }
        }
        return null;
    }

    private static List<KvEntry> sampleValues(List<TelemetryKeySpec> keys) {
        List<KvEntry> entries = new java.util.ArrayList<>();
        for (TelemetryKeySpec key : keys) {
            if (!key.isTimeseries() || key.key() == null) {
                continue;
            }
            KvEntry entry = sample(key);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static KvEntry sample(TelemetryKeySpec key) {
        double min = key.minValue() != null ? key.minValue() : 0.0d;
        double max = key.maxValue() != null ? key.maxValue() : 100.0d;
        if (max < min) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return switch (key.dataType() != null ? key.dataType() : TelemetryKeySpec.DataType.DOUBLE) {
            case DOUBLE -> new DoubleDataEntry(key.key(), round(ThreadLocalRandom.current().nextDouble(min, max + 1e-9)));
            case LONG -> new LongDataEntry(key.key(), (long) ThreadLocalRandom.current().nextDouble(min, max + 1e-9));
            case BOOLEAN -> new BooleanDataEntry(key.key(), ThreadLocalRandom.current().nextBoolean());
            case STRING -> null;
        };
    }

    private static double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

}
