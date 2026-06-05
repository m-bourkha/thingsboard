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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface AddCustomersToGroupDialogData {
  groupId: string;
}

@Component({
  selector: 'tb-add-customers-to-group-dialog',
  templateUrl: './add-customers-to-group-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddCustomersToGroupDialogComponent}],
  standalone: false,
  styleUrls: []
})
export class AddCustomersToGroupDialogComponent extends
  DialogComponent<AddCustomersToGroupDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  formGroup: UntypedFormGroup;
  submitted = false;
  readonly entityType = EntityType;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: AddCustomersToGroupDialogData,
    private entityGroupService: EntityGroupService,
    @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
    public dialogRef: MatDialogRef<AddCustomersToGroupDialogComponent, boolean>,
    public fb: UntypedFormBuilder
  ) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.formGroup = this.fb.group({
      entityIds: [null, [Validators.required]]
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  add(): void {
    this.submitted = true;
    const entityIds: string[] = this.formGroup.get('entityIds').value;
    const ids: EntityId[] = entityIds.map(id => ({ entityType: EntityType.CUSTOMER, id }));
    this.entityGroupService.addEntitiesToGroup(this.data.groupId, ids).subscribe(
      () => this.dialogRef.close(true)
    );
  }
}
