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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  defaultEntityGroupColumns,
  defaultEntityGroupSettings,
  EntityGroup,
  EntityGroupColumnConfiguration,
  EntityGroupSettings
} from '@shared/models/entity-group.model';

@Component({
  selector: 'tb-entity-group',
  templateUrl: './entity-group.component.html',
  standalone: false,

  styleUrls: []
})
export class EntityGroupComponent extends EntityComponent<EntityGroup> {

  constructor(protected store: Store<AppState>,
              @Inject('entity') protected entityValue: EntityGroup,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EntityGroup>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: EntityGroup): UntypedFormGroup {
    const s = this.settingsValue(entity);
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      publicGroup: [entity ? entity.publicGroup : false],
      additionalInfo: this.fb.group({
        description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
      }),
      configuration: this.fb.group({
        columns: [this.columnsValue(entity)],
        settings: this.fb.group({
          enableSearch:              [s.enableSearch],
          enableAdd:                 [s.enableAdd],
          enableDelete:              [s.enableDelete],
          enableBulkOps:             [s.enableBulkOps],
          enableGroupTransfer:       [s.enableGroupTransfer],
          tableTitle:                [s.tableTitle],
          openDetailsOn:             [s.openDetailsOn],
          displayPagination:         [s.displayPagination],
          pageStepIncrement:         [s.pageStepIncrement, [Validators.required, Validators.min(1)]],
          numberOfSteps:             [s.numberOfSteps,     [Validators.required, Validators.min(1)]],
          defaultPageSize:           [s.defaultPageSize,   [Validators.required, Validators.min(1)]],
          enableUsersManagement:     [s.enableUsersManagement],
          enableCustomersManagement: [s.enableCustomersManagement],
          enableAssetsManagement:    [s.enableAssetsManagement],
          enableDevicesManagement:   [s.enableDevicesManagement],
          enableViewsManagement:     [s.enableViewsManagement],
          enableEdgesManagement:     [s.enableEdgesManagement],
          enableDashboardsManagement:[s.enableDashboardsManagement]
        }),
        actions: [entity?.configuration?.actions ?? []]
      })
    });
  }

  updateForm(entity: EntityGroup) {
    this.entityForm.patchValue({
      name: entity.name,
      publicGroup: entity.publicGroup,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      },
      configuration: {
        columns: this.columnsValue(entity),
        settings: this.settingsValue(entity),
        actions: entity?.configuration?.actions ?? []
      }
    });
  }

  private settingsValue(entity: EntityGroup): EntityGroupSettings {
    return { ...defaultEntityGroupSettings(), ...(entity?.configuration?.settings ?? {}) };
  }

  private columnsValue(entity: EntityGroup): EntityGroupColumnConfiguration[] {
    const columns = entity?.configuration?.columns;
    return columns && columns.length ? columns : defaultEntityGroupColumns(entity?.type);
  }
}
