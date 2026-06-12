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

import { ChangeDetectorRef, Component, DestroyRef, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  GenericPermission,
  operations,
  operationTranslations,
  resources,
  resourceTranslations,
  Role,
  RolePermissions,
  RoleType,
  roleTypeTranslations
} from '@shared/models/role.model';

@Component({
  selector: 'tb-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.scss'],
  standalone: false
})
export class RoleComponent extends EntityComponent<Role> {

  roleTypes = Object.values(RoleType);
  roleTypeTranslations = roleTypeTranslations;

  operations = operations;
  operationTranslations = operationTranslations;

  resources = resources;
  resourceTranslations = resourceTranslations;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              @Inject('entity') protected entityValue: Role,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  get genericArray(): UntypedFormArray {
    return this.entityForm.get('generic') as UntypedFormArray;
  }

  get isGroup(): boolean {
    return this.entityForm.get('type').value === RoleType.GROUP;
  }

  buildForm(entity: Role): UntypedFormGroup {
    const permissions: RolePermissions = entity?.permissions ?? {};
    const form = this.fb.group({
      name: [entity?.name ?? '', [Validators.required, Validators.maxLength(255), Validators.pattern(/.*\S.*/)]],
      type: [entity?.type ?? RoleType.GENERIC, [Validators.required]],
      description: [entity?.description ?? ''],
      operations: [permissions.operations ?? []],
      generic: this.fb.array((permissions.generic ?? []).map(p => this.createGenericRow(p)))
    });
    form.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateFormState());
    return form;
  }

  updateForm(entity: Role) {
    const permissions: RolePermissions = entity?.permissions ?? {};
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type ?? RoleType.GENERIC,
      description: entity.description,
      operations: permissions.operations ?? []
    }, {emitEvent: false});
    const genericArray = this.genericArray;
    genericArray.clear({emitEvent: false});
    (permissions.generic ?? []).forEach(p => genericArray.push(this.createGenericRow(p), {emitEvent: false}));
    this.updateFormState();
  }

  updateFormState() {
    super.updateFormState();
    if (this.entityForm) {
      this.updatePermissionsValidation(this.entityForm.get('type').value);
    }
  }

  prepareFormValue(formValue: any): any {
    const type: RoleType = formValue.type;
    formValue.permissions = type === RoleType.GROUP
      ? {operations: formValue.operations ?? []}
      : {generic: formValue.generic ?? []};
    delete formValue.operations;
    delete formValue.generic;
    return super.prepareFormValue(formValue);
  }

  addGenericRow(): void {
    this.genericArray.push(this.createGenericRow());
    this.entityForm.markAsDirty();
  }

  removeGenericRow(index: number): void {
    this.genericArray.removeAt(index);
    this.entityForm.markAsDirty();
  }

  onRoleIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('role.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private createGenericRow(permission?: GenericPermission): UntypedFormGroup {
    return this.fb.group({
      resource: [permission?.resource ?? null, [Validators.required]],
      allowed: [permission?.allowed ?? []],
      denied: [permission?.denied ?? []]
    });
  }

  private updatePermissionsValidation(type: RoleType): void {
    const operationsControl = this.entityForm.get('operations');
    if (type === RoleType.GROUP) {
      operationsControl.setValidators([Validators.required]);
      this.genericArray.disable({emitEvent: false});
      if (this.isEditValue) {
        operationsControl.enable({emitEvent: false});
      }
    } else {
      operationsControl.clearValidators();
      operationsControl.disable({emitEvent: false});
      if (this.isEditValue) {
        this.genericArray.enable({emitEvent: false});
      }
    }
    operationsControl.updateValueAndValidity({emitEvent: false});
  }
}
