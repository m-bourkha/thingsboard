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

import { ChangeDetectorRef, Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import {
  defaultEntityGroupColumn,
  EntityGroupColumnConfiguration,
  EntityGroupColumnType,
  entityGroupColumnTypeTranslations,
  entityGroupEntityFieldsByType,
  entityGroupSortOrderTranslations
} from '@shared/models/entity-group.model';
import {
  EntityGroupColumnDialogComponent,
  EntityGroupColumnDialogData
} from '@home/components/group/entity-group-column-dialog.component';

@Component({
  selector: 'tb-entity-group-columns',
  templateUrl: './entity-group-columns.component.html',
  styleUrls: ['./entity-group-columns.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupColumnsComponent),
      multi: true
    }
  ]
})
export class EntityGroupColumnsComponent implements ControlValueAccessor {

  @Input() entityType: EntityType;

  columns: EntityGroupColumnConfiguration[] = [];
  disabled = false;

  entityGroupColumnTypeTranslations = entityGroupColumnTypeTranslations;
  entityGroupSortOrderTranslations = entityGroupSortOrderTranslations;

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

  writeValue(columns: EntityGroupColumnConfiguration[]): void {
    this.columns = columns ? [...columns] : [];
    this.cd.markForCheck();
  }

  valueLabel(column: EntityGroupColumnConfiguration): string {
    if (column.type === EntityGroupColumnType.ENTITY_FIELD) {
      const field = entityGroupEntityFieldsByType(this.entityType).find(f => f.value === column.key);
      return field ? field.name : column.key;
    }
    return column.key;
  }

  addColumn(): void {
    this.openColumnDialog(defaultEntityGroupColumn(), true);
  }

  editColumn(index: number): void {
    if (this.disabled) {
      return;
    }
    this.openColumnDialog({ ...this.columns[index] }, false, index);
  }

  removeColumn(index: number): void {
    this.columns.splice(index, 1);
    this.updateModel();
  }

  columnDrop(event: CdkDragDrop<EntityGroupColumnConfiguration[]>): void {
    moveItemInArray(this.columns, event.previousIndex, event.currentIndex);
    this.updateModel();
  }

  private openColumnDialog(column: EntityGroupColumnConfiguration, isAdd: boolean, index?: number): void {
    this.dialog.open<EntityGroupColumnDialogComponent, EntityGroupColumnDialogData, EntityGroupColumnConfiguration>(
      EntityGroupColumnDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { column, isAdd, entityType: this.entityType }
      }).afterClosed().subscribe((result) => {
        if (result) {
          if (isAdd) {
            this.columns.push(result);
          } else {
            this.columns[index] = result;
          }
          this.updateModel();
        }
      });
  }

  private updateModel(): void {
    this.columns = [...this.columns];
    this.propagateChange(this.columns);
    this.cd.markForCheck();
  }
}
