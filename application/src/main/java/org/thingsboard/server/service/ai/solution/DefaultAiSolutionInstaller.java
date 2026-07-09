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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.solution.AiSolutionInstallResult;
import org.thingsboard.server.common.data.ai.solution.AiSolutionInstallResult.Item;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.AlarmSpec;
import org.thingsboard.server.common.data.ai.solution.CalculatedFieldSpec;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.AssetProfileSpec;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.CustomerSpec;
import org.thingsboard.server.common.data.ai.solution.EntityProfilesSpec.DeviceProfileSpec;
import org.thingsboard.server.common.data.ai.solution.IamSpec;
import org.thingsboard.server.common.data.ai.solution.IamSpec.EntityGroupSpec;
import org.thingsboard.server.common.data.ai.solution.IamSpec.GroupPermissionSpec;
import org.thingsboard.server.common.data.ai.solution.IamSpec.RoleSpec;
import org.thingsboard.server.common.data.ai.solution.IamSpec.UserSpec;
import org.thingsboard.server.common.data.ai.solution.InstalledEntity;
import org.thingsboard.server.common.data.ai.solution.TelemetryKeySpec;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.SimpleAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.query.ComplexOperation;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RolePermissions;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic installer that materialises an {@link AiSolutionSpec} as real ThingsBoard entities.
 * Follows the bulk-creation pattern of {@code DefaultSystemDataLoaderService} (raw DAO services) and
 * creates entities in dependency order so foreign keys resolve. Each item is recorded in the result
 * report; a single failure never aborts the run.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultAiSolutionInstaller implements AiSolutionInstaller {

    private final CustomerService customerService;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CalculatedFieldService calculatedFieldService;
    private final RoleService roleService;
    private final EntityGroupService entityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final UserService userService;
    private final AttributesService attributesService;

    @Override
    public Outcome install(SecurityUser user, AiSolutionSpec spec) {
        Ctx ctx = new Ctx(user.getTenantId());
        // Order matters: later steps reference entities created by earlier ones.
        installCustomers(ctx, spec);
        installDeviceProfiles(ctx, spec);
        installAssetProfiles(ctx, spec);
        installEntityGroups(ctx, spec);
        installRoles(ctx, spec);
        installUsers(ctx, spec);
        installPermissions(ctx, spec);
        installCalculatedFields(ctx, spec);
        installAlarms(ctx, spec);
        return new Outcome(AiSolutionInstallResult.of(ctx.items), ctx.installed);
    }

    // ---------------------------------------------------------------------------------------------
    // Customers
    // ---------------------------------------------------------------------------------------------

    private void installCustomers(Ctx ctx, AiSolutionSpec spec) {
        for (CustomerSpec cs : spec.entityProfilesOrEmpty().customersOrEmpty()) {
            try {
                Customer customer = new Customer();
                customer.setTenantId(ctx.tenantId);
                customer.setTitle(cs.name());
                Customer saved = customerService.saveCustomer(customer);
                ctx.customersByName.put(cs.name(), saved);
                ctx.record("CUSTOMER", cs.name(), saved.getId(), EntityType.CUSTOMER);
                saveAttributes(ctx, saved.getId(), cs.attributesOrEmpty());
            } catch (Exception e) {
                ctx.fail("CUSTOMER", cs.name(), e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Device profiles + sample devices
    // ---------------------------------------------------------------------------------------------

    private void installDeviceProfiles(Ctx ctx, AiSolutionSpec spec) {
        for (DeviceProfileSpec ps : spec.entityProfilesOrEmpty().deviceProfilesOrEmpty()) {
            DeviceProfile saved;
            try {
                DeviceProfile profile = new DeviceProfile();
                profile.setTenantId(ctx.tenantId);
                profile.setName(ps.name());
                profile.setType(DeviceProfileType.DEFAULT);
                profile.setTransportType(DeviceTransportType.DEFAULT);
                profile.setProvisionType(DeviceProfileProvisionType.DISABLED);
                profile.setDescription(ps.description());
                profile.setProfileData(defaultDeviceProfileData());
                saved = deviceProfileService.saveDeviceProfile(profile);
                ctx.deviceProfilesByName.put(ps.name(), saved);
                ctx.record("DEVICE_PROFILE", ps.name(), saved.getId(), EntityType.DEVICE_PROFILE);
            } catch (Exception e) {
                ctx.fail("DEVICE_PROFILE", ps.name(), e);
                continue;
            }
            CustomerId ownerCustomer = resolveCustomerId(ctx, ps.owner());
            for (int i = 1; i <= ps.sampleCountOrDefault(); i++) {
                String deviceName = ps.name() + " " + i;
                try {
                    Device device = new Device();
                    device.setTenantId(ctx.tenantId);
                    device.setName(deviceName);
                    device.setDeviceProfileId(saved.getId());
                    device.setType(saved.getName());
                    if (ownerCustomer != null) {
                        device.setCustomerId(ownerCustomer);
                    }
                    Device savedDevice = deviceService.saveDevice(device);
                    ctx.devices.add(new Created(savedDevice.getId(), ps.owner(), deviceName));
                    ctx.record("DEVICE", deviceName, savedDevice.getId(), EntityType.DEVICE);
                    saveAttributes(ctx, savedDevice.getId(), attributeKeys(ps.telemetryOrEmpty()));
                } catch (Exception e) {
                    ctx.fail("DEVICE", deviceName, e);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Asset profiles + sample assets
    // ---------------------------------------------------------------------------------------------

    private void installAssetProfiles(Ctx ctx, AiSolutionSpec spec) {
        for (AssetProfileSpec ps : spec.entityProfilesOrEmpty().assetProfilesOrEmpty()) {
            AssetProfile saved;
            try {
                AssetProfile profile = new AssetProfile();
                profile.setTenantId(ctx.tenantId);
                profile.setName(ps.name());
                profile.setDescription(ps.description());
                saved = assetProfileService.saveAssetProfile(profile);
                ctx.assetProfilesByName.put(ps.name(), saved);
                ctx.record("ASSET_PROFILE", ps.name(), saved.getId(), EntityType.ASSET_PROFILE);
            } catch (Exception e) {
                ctx.fail("ASSET_PROFILE", ps.name(), e);
                continue;
            }
            CustomerId ownerCustomer = resolveCustomerId(ctx, ps.owner());
            for (int i = 1; i <= ps.sampleCountOrDefault(); i++) {
                String assetName = ps.name() + " " + i;
                try {
                    Asset asset = new Asset();
                    asset.setTenantId(ctx.tenantId);
                    asset.setName(assetName);
                    asset.setAssetProfileId(saved.getId());
                    asset.setType(saved.getName());
                    if (ownerCustomer != null) {
                        asset.setCustomerId(ownerCustomer);
                    }
                    Asset savedAsset = assetService.saveAsset(asset);
                    ctx.assets.add(new Created(savedAsset.getId(), ps.owner(), assetName));
                    ctx.record("ASSET", assetName, savedAsset.getId(), EntityType.ASSET);
                    saveAttributes(ctx, savedAsset.getId(), attributeKeys(ps.telemetryOrEmpty()));
                } catch (Exception e) {
                    ctx.fail("ASSET", assetName, e);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Entity groups (and membership of already-created device/asset/customer entities)
    // ---------------------------------------------------------------------------------------------

    private void installEntityGroups(Ctx ctx, AiSolutionSpec spec) {
        for (EntityGroupSpec gs : spec.iamOrEmpty().entityGroupsOrEmpty()) {
            EntityType type = parseEntityType(gs.entityType());
            if (type == null) {
                ctx.skip("ENTITY_GROUP", gs.name(), "unsupported entity type: " + gs.entityType());
                continue;
            }
            try {
                EntityId ownerId = "TENANT".equalsIgnoreCase(nullSafe(gs.owner())) || gs.owner() == null
                        ? ctx.tenantId
                        : ownerIdForCustomer(ctx, gs.owner());
                EntityGroup group = new EntityGroup();
                group.setTenantId(ctx.tenantId);
                group.setOwnerId(ownerId);
                group.setType(type);
                group.setName(gs.name());
                EntityGroup saved = entityGroupService.saveEntityGroup(group);
                ctx.entityGroupsByName.put(gs.name(), saved);
                ctx.record("ENTITY_GROUP", gs.name(), saved.getId(), EntityType.ENTITY_GROUP);
                populateGroup(ctx, saved, gs.owner());
            } catch (Exception e) {
                ctx.fail("ENTITY_GROUP", gs.name(), e);
            }
        }
    }

    private void populateGroup(Ctx ctx, EntityGroup group, String ownerName) {
        // USER groups are populated when users are created; DEVICE/ASSET/CUSTOMER groups get all
        // created entities of the same type and owner as a sensible default membership.
        List<Created> members = switch (group.getType()) {
            case DEVICE -> ctx.devices;
            case ASSET -> ctx.assets;
            case CUSTOMER -> new ArrayList<>(ctx.customersByName.values().stream()
                    .map(c -> new Created(c.getId(), null, c.getTitle())).toList());
            default -> List.of();
        };
        for (Created member : members) {
            if (group.getType() == EntityType.CUSTOMER || sameOwner(member.ownerName(), ownerName)) {
                try {
                    entityGroupService.addEntityToGroup(group.getId(), member.id());
                } catch (Exception e) {
                    log.debug("Failed to add {} to group {}", member.id(), group.getName(), e);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Roles
    // ---------------------------------------------------------------------------------------------

    private void installRoles(Ctx ctx, AiSolutionSpec spec) {
        for (RoleSpec rs : spec.iamOrEmpty().rolesOrEmpty()) {
            try {
                Role role = new Role();
                role.setTenantId(ctx.tenantId);
                role.setName(rs.name());
                role.setDescription(rs.description());
                RoleSpec.RoleKind kind = rs.type() != null ? rs.type() : RoleSpec.RoleKind.GROUP;
                role.setType(kind == RoleSpec.RoleKind.GENERIC ? RoleType.GENERIC : RoleType.GROUP);
                role.setPermissions(buildRolePermissions(rs, kind));
                Role saved = roleService.saveRole(role);
                ctx.rolesByName.put(rs.name(), saved);
                ctx.record("ROLE", rs.name(), saved.getId(), EntityType.ROLE);
            } catch (Exception e) {
                ctx.fail("ROLE", rs.name(), e);
            }
        }
    }

    private RolePermissions buildRolePermissions(RoleSpec rs, RoleSpec.RoleKind kind) {
        RolePermissions permissions = new RolePermissions();
        if (kind == RoleSpec.RoleKind.GENERIC) {
            List<RolePermissions.GenericPermission> generic = new ArrayList<>();
            for (IamSpec.GenericPermissionSpec gp : rs.genericOrEmpty()) {
                if (!isValidResource(gp.resource())) {
                    continue;
                }
                RolePermissions.GenericPermission perm = new RolePermissions.GenericPermission();
                perm.setResource(gp.resource());
                perm.setAllowed(validOperations(gp.allowedOrEmpty()));
                perm.setDenied(validOperations(gp.deniedOrEmpty()));
                generic.add(perm);
            }
            permissions.setGeneric(generic);
        } else {
            permissions.setOperations(validOperations(rs.operationsOrEmpty()));
        }
        return permissions;
    }

    // ---------------------------------------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------------------------------------

    private void installUsers(Ctx ctx, AiSolutionSpec spec) {
        for (UserSpec us : spec.iamOrEmpty().usersOrEmpty()) {
            try {
                Authority authority = "TENANT_ADMIN".equalsIgnoreCase(nullSafe(us.authority()))
                        ? Authority.TENANT_ADMIN : Authority.CUSTOMER_USER;
                CustomerId customerId = resolveCustomerId(ctx, us.customer());
                if (authority == Authority.CUSTOMER_USER && customerId == null) {
                    ctx.skip("USER", us.email(), "customer '" + us.customer() + "' not found for customer user");
                    continue;
                }
                User user = new User();
                user.setTenantId(ctx.tenantId);
                user.setAuthority(authority);
                user.setEmail(us.email());
                user.setFirstName(us.firstName());
                user.setLastName(us.lastName());
                if (customerId != null) {
                    user.setCustomerId(customerId);
                }
                User saved = userService.saveUser(ctx.tenantId, user);
                ctx.record("USER", us.email(), saved.getId(), EntityType.USER);
                addUserToGroup(ctx, saved.getId(), us.userGroup());
            } catch (Exception e) {
                ctx.fail("USER", us.email(), e);
            }
        }
    }

    private void addUserToGroup(Ctx ctx, UserId userId, String userGroupName) {
        if (userGroupName == null) {
            return;
        }
        EntityGroup group = ctx.entityGroupsByName.get(userGroupName);
        if (group != null && group.getType() == EntityType.USER) {
            try {
                entityGroupService.addEntityToGroup(group.getId(), userId);
            } catch (Exception e) {
                log.debug("Failed to add user {} to group {}", userId, userGroupName, e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Group permissions
    // ---------------------------------------------------------------------------------------------

    private void installPermissions(Ctx ctx, AiSolutionSpec spec) {
        for (GroupPermissionSpec ps : spec.iamOrEmpty().permissionsOrEmpty()) {
            String label = ps.userGroup() + " -> " + ps.role();
            try {
                EntityGroup userGroup = ctx.entityGroupsByName.get(ps.userGroup());
                Role role = ctx.rolesByName.get(ps.role());
                if (userGroup == null || role == null) {
                    ctx.skip("GROUP_PERMISSION", label, "user group or role not found");
                    continue;
                }
                GroupPermission permission = new GroupPermission();
                permission.setTenantId(ctx.tenantId);
                permission.setUserGroupId(userGroup.getId());
                permission.setRoleId((RoleId) role.getId());
                if (role.getType() == RoleType.GROUP && ps.entityGroup() != null) {
                    EntityGroup entityGroup = ctx.entityGroupsByName.get(ps.entityGroup());
                    if (entityGroup != null) {
                        permission.setEntityGroupId(entityGroup.getId());
                        permission.setEntityGroupType(entityGroup.getType());
                    }
                }
                GroupPermission saved = groupPermissionService.saveGroupPermission(permission);
                ctx.record("GROUP_PERMISSION", label, saved.getId(), EntityType.GROUP_PERMISSION);
            } catch (Exception e) {
                ctx.fail("GROUP_PERMISSION", label, e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Calculated fields
    // ---------------------------------------------------------------------------------------------

    private void installCalculatedFields(Ctx ctx, AiSolutionSpec spec) {
        for (CalculatedFieldSpec cs : spec.calculatedFieldsOrEmpty()) {
            try {
                EntityId profileId = profileIdByName(ctx, cs.entityProfile());
                if (profileId == null) {
                    ctx.skip("CALCULATED_FIELD", cs.name(), "profile '" + cs.entityProfile() + "' not found");
                    continue;
                }
                CalculatedField cf = new CalculatedField();
                cf.setTenantId(ctx.tenantId);
                cf.setEntityId(profileId);
                cf.setType(CalculatedFieldType.SIMPLE);
                cf.setName(cs.name());

                SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
                Map<String, Argument> arguments = new LinkedHashMap<>();
                for (String arg : cs.argumentsOrEmpty()) {
                    arguments.put(arg, tsLatestArgument(arg));
                }
                config.setArguments(arguments);
                config.setExpression(cs.expression());
                TimeSeriesOutput output = new TimeSeriesOutput();
                output.setName(cs.output() != null ? cs.output() : cs.name());
                config.setOutput(output);
                cf.setConfiguration(config);

                CalculatedField saved = calculatedFieldService.save(cf);
                ctx.record("CALCULATED_FIELD", cs.name(), saved.getId(), EntityType.CALCULATED_FIELD);
            } catch (Exception e) {
                ctx.fail("CALCULATED_FIELD", cs.name(), e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Alarms (modelled as ALARM calculated fields on the device profile)
    // ---------------------------------------------------------------------------------------------

    private void installAlarms(Ctx ctx, AiSolutionSpec spec) {
        for (AlarmSpec as : spec.alarmsOrEmpty()) {
            try {
                DeviceProfile profile = ctx.deviceProfilesByName.get(as.entityProfile());
                if (profile == null) {
                    ctx.skip("ALARM", as.type(), "device profile '" + as.entityProfile() + "' not found");
                    continue;
                }
                CalculatedField cf = new CalculatedField();
                cf.setTenantId(ctx.tenantId);
                cf.setEntityId(profile.getId());
                cf.setType(CalculatedFieldType.ALARM);
                cf.setName(as.type());

                AlarmCalculatedFieldConfiguration config = new AlarmCalculatedFieldConfiguration();
                config.setArguments(Map.of(as.key(), tsLatestArgument(as.key())));

                AlarmSpec.Operation op = as.operation() != null ? as.operation() : AlarmSpec.Operation.GREATER;
                double threshold = as.threshold() != null ? as.threshold() : 0.0d;
                config.setCreateRules(Map.of(severityOrDefault(as.severity()),
                        thresholdRule(as.key(), toNumericOperation(op), threshold)));
                config.setClearRule(thresholdRule(as.key(), toNumericOperation(invert(op)), threshold));
                cf.setConfiguration(config);

                CalculatedField saved = calculatedFieldService.save(cf);
                ctx.record("ALARM", as.type(), saved.getId(), EntityType.CALCULATED_FIELD);
            } catch (Exception e) {
                ctx.fail("ALARM", as.type(), e);
            }
        }
    }

    private AlarmRule thresholdRule(String key, NumericFilterPredicate.NumericOperation operation, double threshold) {
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(operation);
        predicate.setValue(new AlarmConditionValue<>(threshold, null));

        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setArgument(key);
        filter.setValueType(EntityKeyValueType.NUMERIC);
        filter.setPredicates(List.of(predicate));

        SimpleAlarmCondition condition = new SimpleAlarmCondition();
        condition.setExpression(new SimpleAlarmConditionExpression(List.of(filter), ComplexOperation.AND));

        AlarmRule rule = new AlarmRule();
        rule.setCondition(condition);
        rule.setAlarmDetails("${" + key + "}");
        return rule;
    }

    // ---------------------------------------------------------------------------------------------
    // Uninstall
    // ---------------------------------------------------------------------------------------------

    @Override
    public AiSolutionInstallResult uninstall(SecurityUser user, List<InstalledEntity> installedEntities) {
        TenantId tenantId = user.getTenantId();
        List<Item> items = new ArrayList<>();
        // Delete in reverse creation order so dependents go before their dependencies.
        List<InstalledEntity> reversed = new ArrayList<>(installedEntities);
        java.util.Collections.reverse(reversed);
        for (InstalledEntity entity : reversed) {
            try {
                deleteEntity(tenantId, entity);
                items.add(Item.deleted(entity.entityType().name(), entity.name()));
            } catch (Exception e) {
                items.add(Item.failed(entity.entityType().name(), entity.name(), e.getMessage()));
            }
        }
        return AiSolutionInstallResult.of(items);
    }

    private void deleteEntity(TenantId tenantId, InstalledEntity entity) {
        UUID id = entity.id();
        switch (entity.entityType()) {
            case GROUP_PERMISSION -> groupPermissionService.deleteGroupPermission(tenantId, new GroupPermissionId(id));
            case CALCULATED_FIELD -> calculatedFieldService.deleteCalculatedField(tenantId, new CalculatedFieldId(id));
            case USER -> {
                User u = userService.findUserById(tenantId, new UserId(id));
                if (u != null) {
                    userService.deleteUser(tenantId, u);
                }
            }
            case ENTITY_GROUP -> entityGroupService.deleteEntityGroup(tenantId, new EntityGroupId(id));
            case DEVICE -> deviceService.deleteDevice(tenantId, new DeviceId(id));
            case ASSET -> assetService.deleteAsset(tenantId, new AssetId(id));
            case DEVICE_PROFILE -> deviceProfileService.deleteDeviceProfile(tenantId, new DeviceProfileId(id));
            case ASSET_PROFILE -> assetProfileService.deleteAssetProfile(tenantId, new AssetProfileId(id));
            case CUSTOMER -> customerService.deleteCustomer(tenantId, new CustomerId(id));
            case ROLE -> roleService.deleteRole(tenantId, new RoleId(id));
            default -> log.debug("No delete handler for entity type {}", entity.entityType());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static DeviceProfileData defaultDeviceProfileData() {
        DeviceProfileData data = new DeviceProfileData();
        data.setConfiguration(new DefaultDeviceProfileConfiguration());
        data.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        data.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        return data;
    }

    private static Argument tsLatestArgument(String key) {
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey(key, ArgumentType.TS_LATEST, null));
        return argument;
    }

    private CustomerId resolveCustomerId(Ctx ctx, String customerName) {
        if (customerName == null || "TENANT".equalsIgnoreCase(customerName)) {
            return null;
        }
        Customer customer = ctx.customersByName.get(customerName);
        return customer != null ? (CustomerId) customer.getId() : null;
    }

    private EntityId ownerIdForCustomer(Ctx ctx, String customerName) {
        CustomerId customerId = resolveCustomerId(ctx, customerName);
        return customerId != null ? customerId : ctx.tenantId;
    }

    private EntityId profileIdByName(Ctx ctx, String name) {
        DeviceProfile dp = ctx.deviceProfilesByName.get(name);
        if (dp != null) {
            return dp.getId();
        }
        AssetProfile ap = ctx.assetProfilesByName.get(name);
        return ap != null ? ap.getId() : null;
    }

    private void saveAttributes(Ctx ctx, EntityId entityId, List<TelemetryKeySpec> keys) {
        List<AttributeKvEntry> entries = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for (TelemetryKeySpec key : keys) {
            if (key.key() == null || key.defaultValue() == null) {
                continue;
            }
            AttributeKvEntry entry = toAttributeEntry(ts, key);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (!entries.isEmpty()) {
            try {
                attributesService.save(ctx.tenantId, entityId, AttributeScope.SERVER_SCOPE, entries);
            } catch (Exception e) {
                log.debug("Failed to save attributes for {}", entityId, e);
            }
        }
    }

    private static AttributeKvEntry toAttributeEntry(long ts, TelemetryKeySpec key) {
        try {
            return switch (key.dataType() != null ? key.dataType() : TelemetryKeySpec.DataType.STRING) {
                case DOUBLE -> new BaseAttributeKvEntry(ts, new DoubleDataEntry(key.key(), Double.parseDouble(key.defaultValue())));
                case LONG -> new BaseAttributeKvEntry(ts, new LongDataEntry(key.key(), Long.parseLong(key.defaultValue())));
                case BOOLEAN -> new BaseAttributeKvEntry(ts, new BooleanDataEntry(key.key(), Boolean.parseBoolean(key.defaultValue())));
                case STRING -> new BaseAttributeKvEntry(ts, new StringDataEntry(key.key(), key.defaultValue()));
            };
        } catch (NumberFormatException e) {
            return new BaseAttributeKvEntry(ts, new StringDataEntry(key.key(), key.defaultValue()));
        }
    }

    private static List<TelemetryKeySpec> attributeKeys(List<TelemetryKeySpec> keys) {
        return keys.stream().filter(k -> !k.isTimeseries()).toList();
    }

    private static List<String> validOperations(List<String> operations) {
        List<String> valid = new ArrayList<>();
        for (String op : operations) {
            String normalized = "UPDATE".equalsIgnoreCase(op) ? "WRITE" : op;
            if (isValidOperation(normalized)) {
                valid.add(normalized.toUpperCase());
            }
        }
        return valid;
    }

    private static boolean isValidOperation(String op) {
        try {
            Operation.valueOf(op.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidResource(String resource) {
        if (resource == null) {
            return false;
        }
        try {
            Resource.valueOf(resource.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static AlarmSeverity severityOrDefault(String severity) {
        if (severity == null) {
            return AlarmSeverity.CRITICAL;
        }
        try {
            return AlarmSeverity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return AlarmSeverity.CRITICAL;
        }
    }

    private static NumericFilterPredicate.NumericOperation toNumericOperation(AlarmSpec.Operation op) {
        return NumericFilterPredicate.NumericOperation.valueOf(op.name());
    }

    private static AlarmSpec.Operation invert(AlarmSpec.Operation op) {
        return switch (op) {
            case GREATER -> AlarmSpec.Operation.LESS_OR_EQUAL;
            case GREATER_OR_EQUAL -> AlarmSpec.Operation.LESS;
            case LESS -> AlarmSpec.Operation.GREATER_OR_EQUAL;
            case LESS_OR_EQUAL -> AlarmSpec.Operation.GREATER;
            case EQUAL -> AlarmSpec.Operation.NOT_EQUAL;
            case NOT_EQUAL -> AlarmSpec.Operation.EQUAL;
        };
    }

    private static EntityType parseEntityType(String type) {
        if (type == null) {
            return null;
        }
        try {
            EntityType entityType = EntityType.valueOf(type.toUpperCase());
            return switch (entityType) {
                case DEVICE, ASSET, CUSTOMER, USER -> entityType;
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean sameOwner(String a, String b) {
        String na = (a == null || "TENANT".equalsIgnoreCase(a)) ? null : a;
        String nb = (b == null || "TENANT".equalsIgnoreCase(b)) ? null : b;
        return java.util.Objects.equals(na, nb);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Per-run mutable state: name -> created entity lookups and the accumulated report / installed list.
     */
    private static final class Ctx {
        private final TenantId tenantId;
        private final List<Item> items = new ArrayList<>();
        private final List<InstalledEntity> installed = new ArrayList<>();
        private final Map<String, Customer> customersByName = new LinkedHashMap<>();
        private final Map<String, DeviceProfile> deviceProfilesByName = new LinkedHashMap<>();
        private final Map<String, AssetProfile> assetProfilesByName = new LinkedHashMap<>();
        private final Map<String, EntityGroup> entityGroupsByName = new LinkedHashMap<>();
        private final Map<String, Role> rolesByName = new LinkedHashMap<>();
        private final List<Created> devices = new ArrayList<>();
        private final List<Created> assets = new ArrayList<>();

        private Ctx(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        private void record(String type, String name, EntityId id, EntityType entityType) {
            items.add(Item.created(type, name, id.getId().toString(), entityType.name()));
            installed.add(new InstalledEntity(entityType, id.getId(), name));
        }

        private void skip(String type, String name, String message) {
            items.add(Item.skipped(type, name, message));
        }

        private void fail(String type, String name, Exception e) {
            log.debug("Failed to install {} '{}'", type, name, e);
            items.add(Item.failed(type, name, e.getMessage()));
        }
    }

    private record Created(EntityId id, String ownerName, String name) {
    }

}
