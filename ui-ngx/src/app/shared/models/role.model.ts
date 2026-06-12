///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export enum RoleType {
  GENERIC = 'GENERIC',
  GROUP = 'GROUP'
}

export const roleTypeTranslations = new Map<RoleType, string>([
  [RoleType.GENERIC, 'role.generic'],
  [RoleType.GROUP, 'role.group']
]);

/** Mirrors the backend org.thingsboard.server.service.security.permission.Operation enum. */
export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  ASSIGN_TO_CUSTOMER = 'ASSIGN_TO_CUSTOMER',
  UNASSIGN_FROM_CUSTOMER = 'UNASSIGN_FROM_CUSTOMER',
  RPC_CALL = 'RPC_CALL',
  READ_CREDENTIALS = 'READ_CREDENTIALS',
  WRITE_CREDENTIALS = 'WRITE_CREDENTIALS',
  READ_ATTRIBUTES = 'READ_ATTRIBUTES',
  WRITE_ATTRIBUTES = 'WRITE_ATTRIBUTES',
  READ_TELEMETRY = 'READ_TELEMETRY',
  WRITE_TELEMETRY = 'WRITE_TELEMETRY',
  CLAIM_DEVICES = 'CLAIM_DEVICES',
  ASSIGN_TO_TENANT = 'ASSIGN_TO_TENANT',
  READ_CALCULATED_FIELD = 'READ_CALCULATED_FIELD',
  WRITE_CALCULATED_FIELD = 'WRITE_CALCULATED_FIELD'
}

export const operations: Operation[] = Object.values(Operation);

export const operationTranslations = new Map<Operation, string>([
  [Operation.ALL, 'role.operation.all'],
  [Operation.CREATE, 'role.operation.create'],
  [Operation.READ, 'role.operation.read'],
  [Operation.WRITE, 'role.operation.write'],
  [Operation.DELETE, 'role.operation.delete'],
  [Operation.ASSIGN_TO_CUSTOMER, 'role.operation.assign-to-customer'],
  [Operation.UNASSIGN_FROM_CUSTOMER, 'role.operation.unassign-from-customer'],
  [Operation.RPC_CALL, 'role.operation.rpc-call'],
  [Operation.READ_CREDENTIALS, 'role.operation.read-credentials'],
  [Operation.WRITE_CREDENTIALS, 'role.operation.write-credentials'],
  [Operation.READ_ATTRIBUTES, 'role.operation.read-attributes'],
  [Operation.WRITE_ATTRIBUTES, 'role.operation.write-attributes'],
  [Operation.READ_TELEMETRY, 'role.operation.read-telemetry'],
  [Operation.WRITE_TELEMETRY, 'role.operation.write-telemetry'],
  [Operation.CLAIM_DEVICES, 'role.operation.claim-devices'],
  [Operation.ASSIGN_TO_TENANT, 'role.operation.assign-to-tenant'],
  [Operation.READ_CALCULATED_FIELD, 'role.operation.read-calculated-field'],
  [Operation.WRITE_CALCULATED_FIELD, 'role.operation.write-calculated-field']
]);

/** Mirrors the backend org.thingsboard.server.service.security.permission.Resource enum. */
export enum Resource {
  ALL = 'ALL',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  ALARM = 'ALARM',
  DEVICE = 'DEVICE',
  ASSET = 'ASSET',
  CUSTOMER = 'CUSTOMER',
  DASHBOARD = 'DASHBOARD',
  ENTITY_VIEW = 'ENTITY_VIEW',
  TENANT = 'TENANT',
  RULE_CHAIN = 'RULE_CHAIN',
  USER = 'USER',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  OAUTH2_CLIENT = 'OAUTH2_CLIENT',
  DOMAIN = 'DOMAIN',
  MOBILE_APP = 'MOBILE_APP',
  MOBILE_APP_BUNDLE = 'MOBILE_APP_BUNDLE',
  OAUTH2_CONFIGURATION_TEMPLATE = 'OAUTH2_CONFIGURATION_TEMPLATE',
  TENANT_PROFILE = 'TENANT_PROFILE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  OTA_PACKAGE = 'OTA_PACKAGE',
  EDGE = 'EDGE',
  RPC = 'RPC',
  QUEUE = 'QUEUE',
  VERSION_CONTROL = 'VERSION_CONTROL',
  NOTIFICATION = 'NOTIFICATION',
  MOBILE_APP_SETTINGS = 'MOBILE_APP_SETTINGS',
  JOB = 'JOB',
  AI_MODEL = 'AI_MODEL',
  API_KEY = 'API_KEY',
  ENTITY_GROUP = 'ENTITY_GROUP',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION'
}

export const resources: Resource[] = Object.values(Resource);

export const resourceTranslations = new Map<Resource, string>([
  [Resource.ALL, 'role.resources.all'],
  [Resource.ADMIN_SETTINGS, 'role.resources.admin-settings'],
  [Resource.ALARM, 'role.resources.alarm'],
  [Resource.DEVICE, 'role.resources.device'],
  [Resource.ASSET, 'role.resources.asset'],
  [Resource.CUSTOMER, 'role.resources.customer'],
  [Resource.DASHBOARD, 'role.resources.dashboard'],
  [Resource.ENTITY_VIEW, 'role.resources.entity-view'],
  [Resource.TENANT, 'role.resources.tenant'],
  [Resource.RULE_CHAIN, 'role.resources.rule-chain'],
  [Resource.USER, 'role.resources.user'],
  [Resource.WIDGETS_BUNDLE, 'role.resources.widgets-bundle'],
  [Resource.WIDGET_TYPE, 'role.resources.widget-type'],
  [Resource.OAUTH2_CLIENT, 'role.resources.oauth2-client'],
  [Resource.DOMAIN, 'role.resources.domain'],
  [Resource.MOBILE_APP, 'role.resources.mobile-app'],
  [Resource.MOBILE_APP_BUNDLE, 'role.resources.mobile-app-bundle'],
  [Resource.OAUTH2_CONFIGURATION_TEMPLATE, 'role.resources.oauth2-configuration-template'],
  [Resource.TENANT_PROFILE, 'role.resources.tenant-profile'],
  [Resource.DEVICE_PROFILE, 'role.resources.device-profile'],
  [Resource.ASSET_PROFILE, 'role.resources.asset-profile'],
  [Resource.API_USAGE_STATE, 'role.resources.api-usage-state'],
  [Resource.TB_RESOURCE, 'role.resources.tb-resource'],
  [Resource.OTA_PACKAGE, 'role.resources.ota-package'],
  [Resource.EDGE, 'role.resources.edge'],
  [Resource.RPC, 'role.resources.rpc'],
  [Resource.QUEUE, 'role.resources.queue'],
  [Resource.VERSION_CONTROL, 'role.resources.version-control'],
  [Resource.NOTIFICATION, 'role.resources.notification'],
  [Resource.MOBILE_APP_SETTINGS, 'role.resources.mobile-app-settings'],
  [Resource.JOB, 'role.resources.job'],
  [Resource.AI_MODEL, 'role.resources.ai-model'],
  [Resource.API_KEY, 'role.resources.api-key'],
  [Resource.ENTITY_GROUP, 'role.resources.entity-group'],
  [Resource.ROLE, 'role.resources.role'],
  [Resource.GROUP_PERMISSION, 'role.resources.group-permission']
]);

/** A single generic-role permission row: a resource with its allowed and denied operations. */
export interface GenericPermission {
  resource: Resource;
  allowed: Operation[];
  denied: Operation[];
}

/**
 * Type-specific permissions persisted as a free-form JSON object on the backend.
 * `operations` is used by GROUP roles, `generic` by GENERIC roles.
 */
export interface RolePermissions {
  operations?: Operation[];
  generic?: GenericPermission[];
}

export interface Role extends BaseData<RoleId>, HasTenantId, HasVersion, ExportableEntity<RoleId> {
  tenantId?: TenantId;
  name: string;
  type: RoleType;
  description?: string;
  permissions: RolePermissions;
  additionalInfo?: any;
}
