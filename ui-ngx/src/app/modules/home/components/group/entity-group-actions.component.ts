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

import { ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  defaultEntityGroupAction,
  EntityGroupActionConfiguration,
  entityGroupActionSourceTranslations,
  entityGroupActionTypeTranslations
} from '@shared/models/entity-group.model';
import {
  EntityGroupActionDialogComponent,
  EntityGroupActionDialogData
} from '@home/components/group/entity-group-action-dialog.component';

@Component({
  selector: 'tb-entity-group-actions',
  templateUrl: './entity-group-actions.component.html',
  styleUrls: ['./entity-group-actions.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupActionsComponent),
      multi: true
    }
  ]
})
export class EntityGroupActionsComponent implements ControlValueAccessor {

  actions: EntityGroupActionConfiguration[] = [];
  disabled = false;

  entityGroupActionSourceTranslations = entityGroupActionSourceTranslations;
  entityGroupActionTypeTranslations = entityGroupActionTypeTranslations;

  private propagateChange = (_val: any) => {};

  constructor(private dialog: MatDialog,
              private cd: ChangeDetectorRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(actions: EntityGroupActionConfiguration[]): void {
    this.actions = actions ? [...actions] : [];
    this.cd.markForCheck();
  }

  addAction(): void {
    this.openActionDialog(defaultEntityGroupAction(), true);
  }

  editAction(index: number): void {
    if (this.disabled) {
      return;
    }
    this.openActionDialog({ ...this.actions[index] }, false, index);
  }

  removeAction(index: number): void {
    this.actions.splice(index, 1);
    this.updateModel();
  }

  actionDrop(event: CdkDragDrop<EntityGroupActionConfiguration[]>): void {
    moveItemInArray(this.actions, event.previousIndex, event.currentIndex);
    this.updateModel();
  }

  private openActionDialog(action: EntityGroupActionConfiguration, isAdd: boolean, index?: number): void {
    this.dialog.open<EntityGroupActionDialogComponent, EntityGroupActionDialogData, EntityGroupActionConfiguration>(
      EntityGroupActionDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { action, isAdd }
      }).afterClosed().subscribe((result) => {
        if (result) {
          if (isAdd) {
            this.actions.push(result);
          } else {
            this.actions[index] = result;
          }
          this.updateModel();
        }
      });
  }

  private updateModel(): void {
    this.actions = [...this.actions];
    this.propagateChange(this.actions);
this.cd.markForCheck();
  }
}
