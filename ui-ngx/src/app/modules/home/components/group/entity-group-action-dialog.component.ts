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
import {
  EntityGroupActionConfiguration,
  entityGroupActionSources,
  entityGroupActionSourceTranslations,
  entityGroupActionTypes,
  entityGroupActionTypeTranslations
} from '@shared/models/entity-group.model';

export interface EntityGroupActionDialogData {
  action: EntityGroupActionConfiguration;
  isAdd: boolean;
}

@Component({
  selector: 'tb-entity-group-action-dialog',
  templateUrl: './entity-group-action-dialog.component.html',
  standalone: false,
  styleUrls: []
})
export class EntityGroupActionDialogComponent
  extends DialogComponent<EntityGroupActionDialogComponent, EntityGroupActionConfiguration> {

  isAdd: boolean;
  actionForm: UntypedFormGroup;

  entityGroupActionSources = entityGroupActionSources;
  entityGroupActionSourceTranslations = entityGroupActionSourceTranslations;
  entityGroupActionTypes = entityGroupActionTypes;
  entityGroupActionTypeTranslations = entityGroupActionTypeTranslations;

  get isCustomAction(): boolean {
    const type = this.actionForm.get('type')?.value;
    return type === 'CUSTOM_ACTION' || type === 'RUN_JS';
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<EntityGroupActionDialogComponent, EntityGroupActionConfiguration>,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupActionDialogData,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    const action = data.action;
    this.actionForm = this.fb.group({
      source: [action.source || 'CELL_BUTTON', [Validators.required]],
      name: [action.name, [Validators.required, Validators.maxLength(255)]],
      icon: [action.icon || 'more_horiz'],
      type: [action.type || 'CUSTOM_ACTION', [Validators.required]],
      customFunction: [action.customFunction ?? ''],
      payload: [action.payload ?? '']
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.actionForm.markAllAsTouched();
    if (this.actionForm.valid) {
      this.dialogRef.close(deepTrim(this.actionForm.value) as EntityGroupActionConfiguration);
    }
  }
}
