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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepTrim } from '@core/utils';
import {
  GenericPermission,
  Operation,
  operations,
  operationTranslations,
  Resource,
  resources,
  resourceTranslations,
  Role,
  RolePermissions,
  RoleType,
  roleTypeTranslations
} from '@shared/models/role.model';
import { RoleService } from '@core/http/role.service';

export interface RoleDialogData {
  role?: Role;
  isAdd?: boolean;
}

@Component({
    selector: 'tb-role-dialog',
    templateUrl: './role-dialog.component.html',
    styleUrls: ['./role-dialog.component.scss'],
    standalone: false
})
export class RoleDialogComponent extends DialogComponent<RoleDialogComponent, Role> {

  roleFormGroup: FormGroup;

  isAdd = false;

  roleType = RoleType;
  roleTypes = Object.values(RoleType);
  roleTypeTranslations = roleTypeTranslations;

  operations = operations;
  operationTranslations = operationTranslations;

  resources = resources;
  resourceTranslations = resourceTranslations;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RoleDialogComponent, Role>,
              @Inject(MAT_DIALOG_DATA) public data: RoleDialogData,
              private fb: FormBuilder,
              private roleService: RoleService) {
    super(store, router, dialogRef);

    this.isAdd = !!this.data.isAdd;
    const role = this.data.role;
    const permissions: RolePermissions = role?.permissions ?? {};

    this.roleFormGroup = this.fb.group({
      name: [role?.name ?? '', [Validators.required, Validators.maxLength(255), Validators.pattern(/.*\S.*/)]],
      type: [role?.type ?? RoleType.GENERIC, [Validators.required]],
      description: [role?.description ?? ''],
      operations: [permissions.operations ?? []],
      generic: this.fb.array((permissions.generic ?? []).map(p => this.createGenericRow(p)))
    });

    this.roleFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type: RoleType) => this.updatePermissionsValidation(type));

    this.updatePermissionsValidation(this.roleFormGroup.get('type').value);
  }

  get genericArray(): FormArray {
    return this.roleFormGroup.get('generic') as FormArray;
  }

  get isGroup(): boolean {
    return this.roleFormGroup.get('type').value === RoleType.GROUP;
  }

  private createGenericRow(permission?: GenericPermission): FormGroup {
    return this.fb.group({
      resource: [permission?.resource ?? null, [Validators.required]],
      allowed: [permission?.allowed ?? []],
      denied: [permission?.denied ?? []]
    });
  }

  addGenericRow(): void {
    this.genericArray.push(this.createGenericRow());
    this.roleFormGroup.markAsDirty();
  }

  removeGenericRow(index: number): void {
    this.genericArray.removeAt(index);
    this.roleFormGroup.markAsDirty();
  }

  private updatePermissionsValidation(type: RoleType): void {
    const operationsControl = this.roleFormGroup.get('operations');
    if (type === RoleType.GROUP) {
      operationsControl.enable({emitEvent: false});
      operationsControl.setValidators([Validators.required]);
      this.genericArray.disable({emitEvent: false});
    } else {
      operationsControl.disable({emitEvent: false});
      operationsControl.clearValidators();
      this.genericArray.enable({emitEvent: false});
    }
    operationsControl.updateValueAndValidity({emitEvent: false});
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const formValue = this.roleFormGroup.value;
    const type: RoleType = formValue.type;
    const permissions: RolePermissions = type === RoleType.GROUP
      ? {operations: formValue.operations ?? []}
      : {generic: formValue.generic ?? []};

    const role: Role = {
      ...this.data.role,
      name: formValue.name,
      type,
      description: formValue.description,
      permissions
    } as Role;

    this.roleService.saveRole(deepTrim(role)).subscribe(saved => this.dialogRef.close(saved));
  }
}
