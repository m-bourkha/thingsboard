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
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityId } from '@shared/models/id/entity-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityType } from '@shared/models/entity-type.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export enum EntityGroupColumnType {
  ENTITY_FIELD = 'ENTITY_FIELD',
  CLIENT_ATTRIBUTE = 'CLIENT_ATTRIBUTE',
  SHARED_ATTRIBUTE = 'SHARED_ATTRIBUTE',
  SERVER_ATTRIBUTE = 'SERVER_ATTRIBUTE',
  TIMESERIES = 'TIMESERIES'
}

export const entityGroupColumnTypes = Object.values(EntityGroupColumnType);

export const entityGroupColumnTypeTranslations = new Map<EntityGroupColumnType, string>([
  [EntityGroupColumnType.ENTITY_FIELD, 'entityGroup.column.type-entity-field'],
  [EntityGroupColumnType.CLIENT_ATTRIBUTE, 'entityGroup.column.type-client-attribute'],
  [EntityGroupColumnType.SHARED_ATTRIBUTE, 'entityGroup.column.type-shared-attribute'],
  [EntityGroupColumnType.SERVER_ATTRIBUTE, 'entityGroup.column.type-server-attribute'],
  [EntityGroupColumnType.TIMESERIES, 'entityGroup.column.type-timeseries']
]);

export enum EntityGroupSortOrder {
  NONE = 'NONE',
  ASC = 'ASC',
  DESC = 'DESC'
}

export const entityGroupSortOrders = Object.values(EntityGroupSortOrder);

export const entityGroupSortOrderTranslations = new Map<EntityGroupSortOrder, string>([
  [EntityGroupSortOrder.NONE, 'entityGroup.sort-order.none'],
  [EntityGroupSortOrder.ASC, 'entityGroup.sort-order.asc'],
  [EntityGroupSortOrder.DESC, 'entityGroup.sort-order.desc']
]);

export type EntityGroupActionSource = 'CELL_BUTTON' | 'HEADER_BUTTON';

export const entityGroupActionSources: EntityGroupActionSource[] = ['CELL_BUTTON', 'HEADER_BUTTON'];

export const entityGroupActionSourceTranslations = new Map<EntityGroupActionSource, string>([
  ['CELL_BUTTON',   'entityGroup.action.source-cell-button'],
  ['HEADER_BUTTON', 'entityGroup.action.source-header-button']
]);

export type EntityGroupActionType = 'OPEN' | 'RUN_JS' | 'CUSTOM_ACTION';

export const entityGroupActionTypes: EntityGroupActionType[] = ['OPEN', 'RUN_JS', 'CUSTOM_ACTION'];

export const entityGroupActionTypeTranslations = new Map<EntityGroupActionType, string>([
  ['OPEN', 'entityGroup.action.type-open'],
  ['RUN_JS', 'entityGroup.action.type-run-js'],
  ['CUSTOM_ACTION', 'entityGroup.action.type-custom-action']
]);

export interface EntityGroupColumnConfiguration {
  type: EntityGroupColumnType;
  key: string;
  title?: string;
  sortOrder: EntityGroupSortOrder;
  mobileHide: boolean;
}

export type EntityGroupOpenDetailsOn = 'ON_ROW_CLICK' | 'ON_ACTION_BUTTON' | 'DISABLED';

export const entityGroupOpenDetailsOnValues: EntityGroupOpenDetailsOn[] = [
  'ON_ROW_CLICK', 'ON_ACTION_BUTTON', 'DISABLED'
];

export const entityGroupOpenDetailsOnTranslations = new Map<EntityGroupOpenDetailsOn, string>([
  ['ON_ROW_CLICK',    'entityGroup.setting.open-details-on-row-click'],
  ['ON_ACTION_BUTTON','entityGroup.setting.open-details-on-action-button'],
  ['DISABLED',        'entityGroup.setting.open-details-disabled']
]);

export interface EntityGroupSettings {
  enableSearch: boolean;
  enableAdd: boolean;
  enableDelete: boolean;
  enableBulkOps: boolean;
  enableGroupTransfer: boolean;
  tableTitle: string;
  openDetailsOn: EntityGroupOpenDetailsOn;
  displayPagination: boolean;
  pageStepIncrement: number;
  numberOfSteps: number;
  defaultPageSize: number;
  enableUsersManagement: boolean;
  enableCustomersManagement: boolean;
  enableAssetsManagement: boolean;
  enableDevicesManagement: boolean;
  enableViewsManagement: boolean;
  enableEdgesManagement: boolean;
  enableDashboardsManagement: boolean;
}

export interface EntityGroupActionConfiguration {
  source: EntityGroupActionSource;
  name: string;
  icon: string;
  type: EntityGroupActionType;
  customFunction?: string;
  payload?: string;
}

export interface EntityGroupConfiguration {
  columns: EntityGroupColumnConfiguration[];
  settings: EntityGroupSettings;
  actions: EntityGroupActionConfiguration[];
}

export function defaultEntityGroupColumn(): EntityGroupColumnConfiguration {
  return {
    type: EntityGroupColumnType.ENTITY_FIELD,
    key: '',
    title: '',
    sortOrder: EntityGroupSortOrder.NONE,
    mobileHide: false
  };
}

export function defaultEntityGroupSettings(): EntityGroupSettings {
  return {
    enableSearch: true,
    enableAdd: true,
    enableDelete: true,
    enableBulkOps: true,
    enableGroupTransfer: true,
    tableTitle: '',
    openDetailsOn: 'ON_ROW_CLICK',
    displayPagination: true,
    pageStepIncrement: 10,
    numberOfSteps: 3,
    defaultPageSize: 10,
    enableUsersManagement: true,
    enableCustomersManagement: true,
    enableAssetsManagement: true,
    enableDevicesManagement: true,
    enableViewsManagement: true,
    enableEdgesManagement: true,
    enableDashboardsManagement: true
  };
}

export function defaultEntityGroupAction(): EntityGroupActionConfiguration {
  return {
    source: 'CELL_BUTTON',
    name: '',
    icon: 'more_horiz',
    type: 'CUSTOM_ACTION',
    customFunction: '',
    payload: ''
  };
}

export interface EntityGroupEntityField {
  value: string;
  name: string;
}

const commonEntityFields: EntityGroupEntityField[] = [
  { value: 'createdTime', name: 'Created time' },
  { value: 'name', name: 'Name' }
];

export const entityGroupEntityFields = new Map<EntityType, EntityGroupEntityField[]>([
  [EntityType.CUSTOMER, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'title', name: 'Title' },
    { value: 'email', name: 'Email' },
    { value: 'country', name: 'Country' },
    { value: 'state', name: 'State' },
    { value: 'city', name: 'City' },
    { value: 'address', name: 'Address' },
    { value: 'address2', name: 'Address 2' },
    { value: 'zip', name: 'Zip/Postal code' },
    { value: 'phone', name: 'Phone' }
  ]],
  [EntityType.DEVICE, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'name', name: 'Name' },
    { value: 'type', name: 'Device profile' },
    { value: 'label', name: 'Label' }
  ]],
  [EntityType.ASSET, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'name', name: 'Name' },
    { value: 'type', name: 'Asset profile' },
    { value: 'label', name: 'Label' }
  ]],
  [EntityType.ENTITY_VIEW, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'name', name: 'Name' },
    { value: 'type', name: 'Type' }
  ]],
  [EntityType.USER, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'email', name: 'Email' },
    { value: 'firstName', name: 'First name' },
    { value: 'lastName', name: 'Last name' }
  ]],
  [EntityType.DASHBOARD, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'title', name: 'Title' }
  ]],
  [EntityType.EDGE, [
    { value: 'createdTime', name: 'Created time' },
    { value: 'name', name: 'Name' },
    { value: 'type', name: 'Type' },
    { value: 'label', name: 'Label' }
  ]]
]);

export function entityGroupEntityFieldsByType(entityType: EntityType): EntityGroupEntityField[] {
  return entityGroupEntityFields.get(entityType) || commonEntityFields;
}

const commonDefaultColumnKeys = ['createdTime', 'name'];

export const entityGroupDefaultColumnKeys = new Map<EntityType, string[]>([
  [EntityType.CUSTOMER, ['createdTime', 'title', 'email', 'country', 'city']],
  [EntityType.DEVICE, ['createdTime', 'name', 'type', 'label']],
  [EntityType.ASSET, ['createdTime', 'name', 'type', 'label']],
  [EntityType.ENTITY_VIEW, ['createdTime', 'name', 'type']],
  [EntityType.USER, ['createdTime', 'email', 'firstName', 'lastName']],
  [EntityType.DASHBOARD, ['createdTime', 'title']],
  [EntityType.EDGE, ['createdTime', 'name', 'type', 'label']]
]);

export function defaultEntityGroupColumns(entityType: EntityType): EntityGroupColumnConfiguration[] {
  const keys = entityGroupDefaultColumnKeys.get(entityType) || commonDefaultColumnKeys;
  return keys.map(key => ({
    type: EntityGroupColumnType.ENTITY_FIELD,
    key,
    title: '',
    sortOrder: key === 'createdTime' ? EntityGroupSortOrder.DESC : EntityGroupSortOrder.NONE,
    mobileHide: false
  }));
}

export interface OwnerInfo {
  ownerId: EntityId;
  ownerName?: string;
}

export interface EntityGroup extends BaseData<EntityGroupId>, HasTenantId, HasVersion, ExportableEntity<EntityGroupId> {
  tenantId?: TenantId;
  ownerId?: EntityId;
  type: EntityType;
  name: string;
  groupAll: boolean;
  publicGroup: boolean;
  configuration?: EntityGroupConfiguration;
  additionalInfo?: any;
}
