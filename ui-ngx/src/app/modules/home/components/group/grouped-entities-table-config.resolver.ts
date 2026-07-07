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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import {
  CellActionDescriptor,
  GroupActionDescriptor,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityType, entityTypeTranslations, entityTypeResources } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  defaultEntityGroupColumns,
  EntityGroup
} from '@shared/models/entity-group.model';
import {
  buildEntityGroupTableColumns,
  entityGroupTableSortOrder
} from '@home/components/group/entity-group-table-columns.utils';
import { DeviceTableHeaderComponent } from '@home/pages/device/device-table-header.component';
import { DeviceComponent } from '@home/pages/device/device.component';
import { DeviceTabsComponent } from '@home/pages/device/device-tabs.component';
import { AssetComponent } from '@home/pages/asset/asset.component';
import { AssetTabsComponent } from '@home/pages/asset/asset-tabs.component';
import { MatDialog } from '@angular/material/dialog';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { Device } from '@shared/models/device.models';
import { Asset } from '@shared/models/asset.models';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable()
export class GroupedEntitiesTableConfigResolver implements Resolve<EntityTableConfig<BaseData<HasId>>> {

  constructor(
    private entityGroupService: EntityGroupService,
    private deviceService: DeviceService,
    private assetService: AssetService,
    private dialogService: DialogService,
    private translate: TranslateService,
    private datePipe: DatePipe,
    private dialog: MatDialog
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<BaseData<HasId>>> {
    const entityGroupId: string = route.params.entityGroupId;
    const entityType = route.data.entityType as EntityType;

    const config = new EntityTableConfig<BaseData<HasId>>();
    config.entityType = entityType;
    config.entityTranslations = entityTypeTranslations.get(entityType);
    config.entityResources = entityTypeResources.get(entityType);
    config.addEnabled = false;
    config.entitiesDeleteEnabled = false;
    config.deleteEnabled = () => false;
    config.detailsPanelEnabled = false;

    if (entityType === EntityType.DEVICE) {
      config.componentsData = {
        deviceInfoFilter: {}
      };
      config.headerComponent = DeviceTableHeaderComponent;
      config.addEnabled = true;
      config.addEntity = () => this.addDeviceToGroup(entityGroupId);

      // Enable the device details side panel, like the All devices tab.
      config.detailsPanelEnabled = true;
      config.entityComponent = DeviceComponent;
      config.entityTabsComponent = DeviceTabsComponent;
      config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
      config.saveEntity = device => this.deviceService.saveDevice(device as Device).pipe(
        mergeMap(savedDevice => this.deviceService.getDeviceInfo(savedDevice.id.id)));

      // Per-row and bulk "Remove from group" actions. The enabled group action also
      // surfaces the selection checkboxes column.
      config.cellActionDescriptors = this.configureCellActions(config, entityGroupId);
      config.groupActionDescriptors = this.configureGroupActions(config, entityGroupId);
    }

    if (entityType === EntityType.ASSET) {
      // No custom header: the asset-profile filter relies on backend filtering the
      // group-entities endpoint does not support (only device profile is supported there).
      // The default table header still provides the title, search and add button.
      config.addEnabled = true;
      config.addEntity = () => this.addEntityToGroup(config, entityGroupId);

      // Enable the asset details side panel, like the All assets tab.
      config.detailsPanelEnabled = true;
      config.entityComponent = AssetComponent;
      config.entityTabsComponent = AssetTabsComponent;
      config.loadEntity = id => this.assetService.getAssetInfo(id.id);
      config.saveEntity = asset => this.assetService.saveAsset(asset as Asset).pipe(
        mergeMap(savedAsset => this.assetService.getAssetInfo(savedAsset.id.id)));

      config.cellActionDescriptors = this.configureCellActions(config, entityGroupId);
      config.groupActionDescriptors = this.configureGroupActions(config, entityGroupId);
    }

    config.entitiesFetchFunction = pageLink =>
      this.entityGroupService.getEntitiesByGroup(entityGroupId, pageLink,
        config.componentsData?.deviceInfoFilter?.deviceProfileId?.id);

    return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
      map(group => {
        config.tableTitle = config.entityTranslations?.typePlural
          ? `${group.name}: ${this.translate.instant(config.entityTranslations.typePlural)}`
          : group.name;
        this.buildColumns(config, entityType, group);
        return config;
      })
    );
  }

  private addDeviceToGroup(entityGroupId: string): Observable<Device> {
    return this.dialog.open<DeviceWizardDialogComponent, AddEntityDialogData<BaseData<HasId>>, Device>(
      DeviceWizardDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed().pipe(
      mergeMap((device) => {
        if (device) {
          return this.entityGroupService.addEntitiesToGroup(entityGroupId, [device.id]).pipe(map(() => device));
        }
        return of(null);
      })
    );
  }

  // Generic "Remove from group" actions, shared by every specially-configured entity type
  // (device, asset, ...). The single enabled group action also surfaces the selection checkboxes.
  private configureCellActions(config: EntityTableConfig<BaseData<HasId>>,
                               entityGroupId: string): Array<CellActionDescriptor<BaseData<HasId>>> {
    return [
      {
        name: this.translate.instant('entityGroup.remove-from-group'),
        icon: 'group_remove',
        isEnabled: () => true,
        onAction: ($event, entity) => this.removeEntityFromGroup($event, config, entityGroupId, entity)
      }
    ];
  }

  private configureGroupActions(config: EntityTableConfig<BaseData<HasId>>,
                                entityGroupId: string): Array<GroupActionDescriptor<BaseData<HasId>>> {
    return [
      {
        name: this.translate.instant('entityGroup.remove-from-group'),
        icon: 'group_remove',
        isEnabled: true,
        onAction: ($event, entities) =>
          this.removeEntitiesFromGroup($event, config, entityGroupId, entities)
      }
    ];
  }

  private removeEntityFromGroup($event: Event, config: EntityTableConfig<BaseData<HasId>>,
                                entityGroupId: string, entity: BaseData<HasId>): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entityGroup.remove-entity-from-group-title', {entityName: entity.name}),
      this.translate.instant('entityGroup.remove-entity-from-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.entityGroupService.removeEntitiesFromGroup(entityGroupId, [entity.id as EntityId]).subscribe(
          () => config.updateData()
        );
      }
    });
  }

  private removeEntitiesFromGroup($event: Event, config: EntityTableConfig<BaseData<HasId>>,
                                  entityGroupId: string, entities: BaseData<HasId>[]): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entityGroup.remove-entities-from-group-title', {count: entities.length}),
      this.translate.instant('entityGroup.remove-entities-from-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        const entityIds: EntityId[] = entities.map(entity => entity.id as EntityId);
        this.entityGroupService.removeEntitiesFromGroup(entityGroupId, entityIds).subscribe(
          () => config.updateData()
        );
      }
    });
  }

  // Generic add-to-group: create an entity via the standard add dialog (driven by the
  // configured entityComponent + saveEntity), then add the saved entity to the group.
  private addEntityToGroup(config: EntityTableConfig<BaseData<HasId>>,
                           entityGroupId: string): Observable<BaseData<HasId>> {
    return this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<BaseData<HasId>>, BaseData<HasId>>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entitiesTableConfig: config
        }
      }).afterClosed().pipe(
      mergeMap((entity) => {
        if (entity) {
          return this.entityGroupService.addEntitiesToGroup(entityGroupId, [entity.id as EntityId]).pipe(map(() => entity));
        }
        return of(null);
      })
    );
  }

  private buildColumns(config: EntityTableConfig<BaseData<HasId>>, entityType: EntityType, group: EntityGroup): void {
    const savedColumns = group?.configuration?.columns ?? [];
    // Use the same default columns the Columns tab seeds with, so the tab and grid stay consistent.
    const configuredColumns = savedColumns.length ? savedColumns : defaultEntityGroupColumns(entityType);

    config.columns.push(...buildEntityGroupTableColumns(configuredColumns, entityType, this.datePipe));

    const sortOrder = entityGroupTableSortOrder(configuredColumns);
    if (sortOrder) {
      config.defaultSortOrder = sortOrder;
    }
  }
}
