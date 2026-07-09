///
/// Copyright © 2016-2026 The Thingsboard Authors
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

export type TelemetryType = 'ATTRIBUTE' | 'TIMESERIES';
export type DataType = 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'STRING';

export interface TelemetryKeySpec {
  telemetryType?: TelemetryType;
  key?: string;
  dataType?: DataType;
  minValue?: number;
  maxValue?: number;
  defaultValue?: string;
}

export interface DeviceProfileSpec {
  name?: string;
  description?: string;
  owner?: string;
  sampleCount?: number;
  telemetry?: TelemetryKeySpec[];
}

export interface AssetProfileSpec {
  name?: string;
  description?: string;
  owner?: string;
  sampleCount?: number;
  telemetry?: TelemetryKeySpec[];
}

export interface CustomerSpec {
  name?: string;
  attributes?: TelemetryKeySpec[];
}

export interface EntityProfilesSpec {
  deviceProfiles?: DeviceProfileSpec[];
  assetProfiles?: AssetProfileSpec[];
  customers?: CustomerSpec[];
}

export interface GenericPermissionSpec {
  resource?: string;
  allowed?: string[];
  denied?: string[];
}

export interface RoleSpec {
  name?: string;
  type?: 'GENERIC' | 'GROUP';
  description?: string;
  operations?: string[];
  generic?: GenericPermissionSpec[];
}

export interface EntityGroupSpec {
  name?: string;
  entityType?: string;
  owner?: string;
}

export interface UserSpec {
  email?: string;
  firstName?: string;
  lastName?: string;
  authority?: string;
  customer?: string;
  userGroup?: string;
}

export interface GroupPermissionSpec {
  userGroup?: string;
  role?: string;
  entityGroup?: string;
  entityGroupType?: string;
}

export interface IamSpec {
  roles?: RoleSpec[];
  entityGroups?: EntityGroupSpec[];
  users?: UserSpec[];
  permissions?: GroupPermissionSpec[];
}

export interface CalculatedFieldSpec {
  name?: string;
  entityProfile?: string;
  arguments?: string[];
  expression?: string;
  output?: string;
}

export interface AlarmSpec {
  type?: string;
  entityProfile?: string;
  severity?: string;
  key?: string;
  operation?: string;
  threshold?: number;
}

export type DashboardScope = 'TENANT' | 'CUSTOMER';

export interface DashboardSpec {
  name?: string;
  assignedTo?: string;
  scope?: DashboardScope;
  customer?: string;
  overview?: string;
  description?: string;
  useCases?: string[];
  entityProfiles?: string[];
}

export interface AiSolutionSpec {
  name?: string;
  description?: string;
  entityProfiles?: EntityProfilesSpec;
  iam?: IamSpec;
  calculatedFields?: CalculatedFieldSpec[];
  alarms?: AlarmSpec[];
  dashboards?: DashboardSpec[];
}

export type AiSolutionStatus = 'DRAFT' | 'INSTALLED' | 'PARTIALLY_INSTALLED' | 'FAILED' | 'UNINSTALLED';

export interface InstalledEntity {
  entityType?: string;
  id?: string;
  name?: string;
}

export interface AiSolution {
  id?: string;
  createdTime?: number;
  tenantId?: { id: string };
  version?: number;
  name?: string;
  originalPrompt?: string;
  status?: AiSolutionStatus;
  spec?: AiSolutionSpec;
  installedEntities?: InstalledEntity[];
}

export type InstallStatus = 'CREATED' | 'SKIPPED' | 'DELETED' | 'FAILED';

export interface InstallItem {
  type?: string;
  name?: string;
  status?: InstallStatus;
  message?: string;
  entityId?: string;
  entityType?: string;
}

export interface AiSolutionInstallResult {
  success?: boolean;
  items?: InstallItem[];
}

export interface AiSolutionGenerateRequest {
  modelId: string;
  prompt: string;
}

export interface AiSolutionRefineRequest {
  modelId: string;
  currentSpec: AiSolutionSpec;
  message: string;
}

export interface AiSolutionDashboardRequest {
  modelId: string;
  spec: AiSolutionSpec;
}

export interface SolutionSuggestion {
  icon: string;
  label: string;
  prompt: string;
}

export const solutionSuggestions: SolutionSuggestion[] = [
  {
    icon: 'coffee',
    label: 'Coffee Machine Monitoring',
    prompt: 'Create a solution to monitor a fleet of connected coffee machines, tracking water level, ' +
      'bean level, temperature and cups served, with alarms for low supplies and maintenance needs.'
  },
  {
    icon: 'local_parking',
    label: 'Smart Parking',
    prompt: 'Create a simple smart parking solution to monitor single level indoor parking lots with magnetic ' +
      'sensors. Alarms must be generated for a sensor offline, a low battery, or if a vehicle overstays a ' +
      'designated time limit.'
  },
  {
    icon: 'water_drop',
    label: 'Water Metering',
    prompt: 'Create a water metering solution that tracks consumption, flow rate and pressure per meter, with ' +
      'alarms for leaks (abnormally high flow) and low battery.'
  },
  {
    icon: 'cloud',
    label: 'Weather Monitoring',
    prompt: 'Create a weather monitoring solution with outdoor stations reporting temperature, humidity, ' +
      'pressure, wind speed and rainfall, with alarms for extreme conditions.'
  },
  {
    icon: 'local_shipping',
    label: 'Fleet Geofencing',
    prompt: 'Create a fleet geofencing solution tracking vehicle location, speed and fuel level, with alarms ' +
      'for speeding and geofence breaches.'
  }
];
