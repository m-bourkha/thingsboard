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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.AlarmSpec;
import org.thingsboard.server.common.data.ai.solution.DashboardSpec;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.AssetProfileSpec;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.DeviceProfileSpec;
import org.thingsboard.server.common.data.ai.solution.TelemetryKeySpec;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Expands a {@link DashboardSpec} into a real {@link Dashboard}.
 * <p>
 * The AI never emits widget JSON — it only says which dashboards exist, who they are for and which
 * entity profiles they visualize. This class turns that into a concrete, always-valid dashboard using
 * three stock system widgets:
 * <ul>
 *     <li>{@code system.alarm_widgets.alarms_table} — when the solution declares alarms on a referenced profile;</li>
 *     <li>{@code system.cards.entities_table} — one per referenced profile, listing all of its keys;</li>
 *     <li>{@code system.time_series_chart} — one per referenced profile that has numeric timeseries keys.</li>
 * </ul>
 * Only the widget-config fields that carry data are emitted; every other default is filled in by the
 * UI's {@code DashboardUtilsService.validateAndUpdateDashboard} when the dashboard is opened.
 */
final class AiSolutionDashboardBuilder {

    private static final String ALARMS_TABLE_FQN = "system.alarm_widgets.alarms_table";
    private static final String ENTITIES_TABLE_FQN = "system.cards.entities_table";
    private static final String TIME_SERIES_CHART_FQN = "system.time_series_chart";

    /** Dashboard layouts are laid out on a 24-column grid. */
    private static final int GRID_COLUMNS = 24;
    private static final int HALF_WIDTH = 12;

    private static final List<String> PALETTE = List.of(
            "#2196f3", "#4caf50", "#ef5350", "#ff9800", "#9c27b0", "#00bcd4", "#795548", "#607d8b");

    private AiSolutionDashboardBuilder() {
    }

    static Dashboard build(TenantId tenantId, AiSolutionSpec spec, DashboardSpec dashboardSpec) {
        List<ProfileRef> profiles = resolveProfiles(spec, dashboardSpec);

        // One entity alias per referenced profile; every widget datasource points at one of them.
        ObjectNode entityAliases = JacksonUtil.newObjectNode();
        Map<String, String> aliasIdByProfile = new LinkedHashMap<>();
        for (ProfileRef profile : profiles) {
            String aliasId = UUID.randomUUID().toString();
            aliasIdByProfile.put(profile.name(), aliasId);
            entityAliases.set(aliasId, entityAlias(aliasId, profile));
        }

        ObjectNode widgets = JacksonUtil.newObjectNode();
        ObjectNode layoutWidgets = JacksonUtil.newObjectNode();
        GridCursor cursor = new GridCursor();

        addAlarmsTable(spec, profiles, aliasIdByProfile, widgets, layoutWidgets, cursor);
        for (ProfileRef profile : profiles) {
            String aliasId = aliasIdByProfile.get(profile.name());
            addEntitiesTable(profile, aliasId, widgets, layoutWidgets, cursor);
            addTimeSeriesChart(profile, aliasId, widgets, layoutWidgets, cursor);
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(dashboardSpec.name());
        dashboard.setConfiguration(configuration(dashboardSpec, widgets, layoutWidgets, entityAliases));
        return dashboard;
    }

    // ---------------------------------------------------------------------------------------------
    // Widgets
    // ---------------------------------------------------------------------------------------------

    private static void addAlarmsTable(AiSolutionSpec spec, List<ProfileRef> profiles, Map<String, String> aliasIdByProfile,
                                       ObjectNode widgets, ObjectNode layoutWidgets, GridCursor cursor) {
        Set<String> profileNames = new LinkedHashSet<>();
        for (ProfileRef profile : profiles) {
            profileNames.add(profile.name());
        }
        String alarmProfile = null;
        for (AlarmSpec alarm : spec.alarmsOrEmpty()) {
            if (alarm.entityProfile() != null && profileNames.contains(alarm.entityProfile())) {
                alarmProfile = alarm.entityProfile();
                break;
            }
        }
        if (alarmProfile == null) {
            return;
        }

        ObjectNode config = JacksonUtil.newObjectNode();
        config.set("datasources", JacksonUtil.newArrayNode());
        config.set("alarmSource", alarmSource(aliasIdByProfile.get(alarmProfile)));
        config.set("alarmFilterConfig", alarmFilterConfig());
        config.put("title", "Alarms");
        config.put("showTitle", true);
        config.set("settings", JacksonUtil.newObjectNode());

        addWidget(widgets, layoutWidgets, cursor, ALARMS_TABLE_FQN, "alarm", config, GRID_COLUMNS, 6);
    }

    private static void addEntitiesTable(ProfileRef profile, String aliasId,
                                         ObjectNode widgets, ObjectNode layoutWidgets, GridCursor cursor) {
        ArrayNode dataKeys = JacksonUtil.newArrayNode();
        int color = 0;
        for (TelemetryKeySpec key : profile.telemetry()) {
            if (key.key() == null) {
                continue;
            }
            dataKeys.add(dataKey(key.key(), key.isTimeseries() ? "timeseries" : "attribute", color++));
        }
        if (dataKeys.isEmpty()) {
            return;
        }

        ObjectNode config = JacksonUtil.newObjectNode();
        config.set("datasources", singleDatasource(aliasId, dataKeys));
        config.put("title", profile.name());
        config.put("showTitle", true);
        config.set("settings", JacksonUtil.newObjectNode());

        addWidget(widgets, layoutWidgets, cursor, ENTITIES_TABLE_FQN, "latest", config, HALF_WIDTH, 8);
    }

    private static void addTimeSeriesChart(ProfileRef profile, String aliasId,
                                           ObjectNode widgets, ObjectNode layoutWidgets, GridCursor cursor) {
        ArrayNode dataKeys = JacksonUtil.newArrayNode();
        int color = 0;
        for (TelemetryKeySpec key : profile.telemetry()) {
            if (key.key() != null && key.isTimeseries() && key.isNumeric()) {
                dataKeys.add(dataKey(key.key(), "timeseries", color++));
            }
        }
        if (dataKeys.isEmpty()) {
            return;
        }

        ObjectNode config = JacksonUtil.newObjectNode();
        config.set("datasources", singleDatasource(aliasId, dataKeys));
        config.put("title", profile.name() + " telemetry");
        config.put("showTitle", true);
        config.put("useDashboardTimewindow", true);
        config.set("settings", JacksonUtil.newObjectNode());

        addWidget(widgets, layoutWidgets, cursor, TIME_SERIES_CHART_FQN, "timeseries", config, HALF_WIDTH, 6);
    }

    private static void addWidget(ObjectNode widgets, ObjectNode layoutWidgets, GridCursor cursor,
                                  String typeFullFqn, String type, ObjectNode config, int sizeX, int sizeY) {
        String widgetId = UUID.randomUUID().toString();

        ObjectNode widget = JacksonUtil.newObjectNode();
        widget.put("id", widgetId);
        widget.put("typeFullFqn", typeFullFqn);
        widget.put("type", type);
        widget.put("sizeX", sizeX);
        widget.put("sizeY", sizeY);
        widget.set("config", config);
        widgets.set(widgetId, widget);

        int[] position = cursor.place(sizeX, sizeY);
        ObjectNode layout = JacksonUtil.newObjectNode();
        layout.put("sizeX", sizeX);
        layout.put("sizeY", sizeY);
        layout.put("row", position[0]);
        layout.put("col", position[1]);
        layoutWidgets.set(widgetId, layout);
    }

    // ---------------------------------------------------------------------------------------------
    // Configuration fragments
    // ---------------------------------------------------------------------------------------------

    private static ObjectNode configuration(DashboardSpec dashboardSpec, ObjectNode widgets,
                                            ObjectNode layoutWidgets, ObjectNode entityAliases) {
        ObjectNode mainLayout = JacksonUtil.newObjectNode();
        mainLayout.set("widgets", layoutWidgets);
        mainLayout.set("gridSettings", gridSettings());

        ObjectNode layouts = JacksonUtil.newObjectNode();
        layouts.set("main", mainLayout);

        ObjectNode defaultState = JacksonUtil.newObjectNode();
        defaultState.put("name", dashboardSpec.name());
        defaultState.put("root", true);
        defaultState.set("layouts", layouts);

        ObjectNode states = JacksonUtil.newObjectNode();
        states.set("default", defaultState);

        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("description", dashboardSpec.overview());
        configuration.set("widgets", widgets);
        configuration.set("states", states);
        configuration.set("entityAliases", entityAliases);
        configuration.set("filters", JacksonUtil.newObjectNode());
        configuration.set("timewindow", timewindow());
        configuration.set("settings", dashboardSettings());
        return configuration;
    }

    private static ObjectNode entityAlias(String aliasId, ProfileRef profile) {
        boolean device = profile.entityType() == EntityType.DEVICE;

        ObjectNode filter = JacksonUtil.newObjectNode();
        filter.put("type", device ? "deviceType" : "assetType");
        filter.put("resolveMultiple", true);
        filter.put(device ? "deviceNameFilter" : "assetNameFilter", "");
        ArrayNode types = JacksonUtil.newArrayNode();
        types.add(profile.name());
        filter.set(device ? "deviceTypes" : "assetTypes", types);

        ObjectNode alias = JacksonUtil.newObjectNode();
        alias.put("id", aliasId);
        alias.put("alias", profile.name());
        alias.set("filter", filter);
        return alias;
    }

    private static ArrayNode singleDatasource(String aliasId, ArrayNode dataKeys) {
        ObjectNode datasource = JacksonUtil.newObjectNode();
        datasource.put("type", "entity");
        datasource.put("entityAliasId", aliasId);
        datasource.set("dataKeys", dataKeys);

        ArrayNode datasources = JacksonUtil.newArrayNode();
        datasources.add(datasource);
        return datasources;
    }

    private static ObjectNode alarmSource(String aliasId) {
        ArrayNode dataKeys = JacksonUtil.newArrayNode();
        int color = 0;
        for (String key : List.of("createdTime", "originator", "type", "severity", "status")) {
            dataKeys.add(dataKey(key, "alarm", color++));
        }

        ObjectNode alarmSource = JacksonUtil.newObjectNode();
        alarmSource.put("type", "entity");
        alarmSource.put("name", "alarms");
        alarmSource.put("entityAliasId", aliasId);
        alarmSource.set("dataKeys", dataKeys);
        return alarmSource;
    }

    private static ObjectNode alarmFilterConfig() {
        ArrayNode statusList = JacksonUtil.newArrayNode();
        statusList.add("ACTIVE");

        ObjectNode filterConfig = JacksonUtil.newObjectNode();
        filterConfig.set("statusList", statusList);
        return filterConfig;
    }

    private static ObjectNode dataKey(String key, String type, int colorIndex) {
        ObjectNode dataKey = JacksonUtil.newObjectNode();
        dataKey.put("name", key);
        dataKey.put("type", type);
        dataKey.put("label", label(key));
        dataKey.put("color", PALETTE.get(Math.floorMod(colorIndex, PALETTE.size())));
        dataKey.set("settings", JacksonUtil.newObjectNode());
        return dataKey;
    }

    private static ObjectNode gridSettings() {
        ObjectNode gridSettings = JacksonUtil.newObjectNode();
        gridSettings.put("backgroundColor", "#eeeeee");
        gridSettings.put("color", "rgba(0,0,0,0.870588)");
        gridSettings.put("columns", GRID_COLUMNS);
        gridSettings.put("margin", 10);
        gridSettings.put("outerMargin", true);
        gridSettings.put("autoFillHeight", false);
        gridSettings.put("layoutType", "default");
        return gridSettings;
    }

    private static ObjectNode timewindow() {
        ObjectNode realtime = JacksonUtil.newObjectNode();
        realtime.put("realtimeType", 1);
        realtime.put("interval", 1000);
        realtime.put("timewindowMs", 86400000L);
        realtime.put("quickInterval", "CURRENT_DAY");

        ObjectNode aggregation = JacksonUtil.newObjectNode();
        aggregation.put("type", "NONE");
        aggregation.put("limit", 200);

        ObjectNode timewindow = JacksonUtil.newObjectNode();
        timewindow.put("selectedTab", 0);
        timewindow.set("realtime", realtime);
        timewindow.set("aggregation", aggregation);
        return timewindow;
    }

    private static ObjectNode dashboardSettings() {
        ObjectNode settings = JacksonUtil.newObjectNode();
        settings.put("stateControllerId", "entity");
        settings.put("showTitle", false);
        settings.put("showDashboardsSelect", true);
        settings.put("showEntitiesSelect", true);
        settings.put("showDashboardTimewindow", true);
        settings.put("showDashboardExport", true);
        settings.put("toolbarAlwaysOpen", true);
        return settings;
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Resolves the profile names the AI referenced against the profiles actually defined in the spec.
     * Unknown names are dropped; an empty reference list falls back to every device profile so the
     * dashboard is never blank.
     */
    private static List<ProfileRef> resolveProfiles(AiSolutionSpec spec, DashboardSpec dashboardSpec) {
        Map<String, ProfileRef> byName = new LinkedHashMap<>();
        for (DeviceProfileSpec ps : spec.entityProfilesOrEmpty().deviceProfilesOrEmpty()) {
            byName.put(ps.name(), new ProfileRef(ps.name(), EntityType.DEVICE, ps.telemetryOrEmpty()));
        }
        for (AssetProfileSpec ps : spec.entityProfilesOrEmpty().assetProfilesOrEmpty()) {
            byName.putIfAbsent(ps.name(), new ProfileRef(ps.name(), EntityType.ASSET, ps.telemetryOrEmpty()));
        }

        List<ProfileRef> resolved = new ArrayList<>();
        for (String name : dashboardSpec.entityProfilesOrEmpty()) {
            ProfileRef profile = byName.get(name);
            if (profile != null && resolved.stream().noneMatch(p -> p.name().equals(name))) {
                resolved.add(profile);
            }
        }
        if (resolved.isEmpty()) {
            for (ProfileRef profile : byName.values()) {
                if (profile.entityType() == EntityType.DEVICE) {
                    resolved.add(profile);
                }
            }
        }
        return resolved;
    }

    /** {@code lowBatteryThreshold} -> {@code Low battery threshold}. */
    private static String label(String key) {
        StringBuilder label = new StringBuilder(key.length() + 4);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                label.append(' ').append(Character.toLowerCase(c));
            } else if (i == 0) {
                label.append(Character.toUpperCase(c));
            } else {
                label.append(c);
            }
        }
        return label.toString();
    }

    private record ProfileRef(String name, EntityType entityType, List<TelemetryKeySpec> telemetry) {
    }

    /** Packs widgets left-to-right across a {@value #GRID_COLUMNS}-column grid, wrapping when a row fills up. */
    private static final class GridCursor {

        private int row;
        private int col;
        private int rowHeight;

        /** Reserves a {@code sizeX} x {@code sizeY} slot and returns its {@code {row, col}}. */
        private int[] place(int sizeX, int sizeY) {
            if (col > 0 && col + sizeX > GRID_COLUMNS) {
                row += rowHeight;
                col = 0;
                rowHeight = 0;
            }
            int[] position = {row, col};
            col += sizeX;
            rowHeight = Math.max(rowHeight, sizeY);
            return position;
        }
    }

}
