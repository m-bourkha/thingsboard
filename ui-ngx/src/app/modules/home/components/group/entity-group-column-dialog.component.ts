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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { deepTrim } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EntityGroupColumnConfiguration,
  EntityGroupColumnType,
  entityGroupColumnTypes,
  entityGroupColumnTypeTranslations,
  EntityGroupEntityField,
  entityGroupEntityFieldsByType,
  EntityGroupSortOrder,
  entityGroupSortOrders,
  entityGroupSortOrderTranslations
} from '@shared/models/entity-group.model';

export interface EntityGroupColumnDialogData {
  column: EntityGroupColumnConfiguration;
  isAdd: boolean;
  entityType: EntityType;
}

@Component({
  selector: 'tb-entity-group-column-dialog',
  templateUrl: './entity-group-column-dialog.component.html',
  standalone: false,
  styleUrls: []
})
export class EntityGroupColumnDialogComponent
  extends DialogComponent<EntityGroupColumnDialogComponent, EntityGroupColumnConfiguration> {

  isAdd: boolean;
  columnForm: UntypedFormGroup;

  columnType = EntityGroupColumnType;
  entityGroupColumnTypes = entityGroupColumnTypes;
  entityGroupColumnTypeTranslations = entityGroupColumnTypeTranslations;
  entityGroupSortOrders = entityGroupSortOrders;
  entityGroupSortOrderTranslations = entityGroupSortOrderTranslations;

  entityFields: EntityGroupEntityField[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<EntityGroupColumnDialogComponent, EntityGroupColumnConfiguration>,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupColumnDialogData,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.entityFields = entityGroupEntityFieldsByType(data.entityType);
    const column = data.column;
    this.columnForm = this.fb.group({
      type: [column.type, [Validators.required]],
      key: [column.key, [Validators.required]],
      title: [column.title || ''],
      sortOrder: [column.sortOrder || EntityGroupSortOrder.NONE],
      mobileHide: [column.mobileHide || false]
    });
    this.columnForm.get('type').valueChanges.subscribe(() => {
      this.columnForm.get('key').reset('', { emitEvent: false });
    });
  }

  isEntityField(): boolean {
    return this.columnForm.get('type').value === EntityGroupColumnType.ENTITY_FIELD;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.columnForm.markAllAsTouched();
    if (this.columnForm.valid) {
      this.dialogRef.close(deepTrim(this.columnForm.value) as EntityGroupColumnConfiguration);
    }
  }
}
