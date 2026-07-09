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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface AiSolutionRenameDialogData {
  name: string;
}

@Component({
  selector: 'tb-ai-solution-rename-dialog',
  templateUrl: './ai-solution-rename-dialog.component.html',
  standalone: false
})
export class AiSolutionRenameDialogComponent {

  renameForm: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: AiSolutionRenameDialogData,
              private dialogRef: MatDialogRef<AiSolutionRenameDialogComponent, string>,
              private fb: UntypedFormBuilder) {
    this.renameForm = this.fb.group({
      name: [data.name, [Validators.required, Validators.maxLength(255)]]
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  rename(): void {
    if (this.renameForm.invalid) {
      return;
    }
    this.dialogRef.close(this.renameForm.get('name').value.trim());
  }

}
