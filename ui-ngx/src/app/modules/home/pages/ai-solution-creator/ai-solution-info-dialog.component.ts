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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AiSolution, InstalledEntity } from '@shared/models/ai-solution-creator.models';

export interface AiSolutionInfoDialogData {
  solution: AiSolution;
}

@Component({
  selector: 'tb-ai-solution-info-dialog',
  templateUrl: './ai-solution-info-dialog.component.html',
  styleUrls: ['./ai-solution-info-dialog.component.scss'],
  standalone: false
})
export class AiSolutionInfoDialogComponent {

  readonly solution: AiSolution;
  readonly installedEntities: InstalledEntity[];

  constructor(@Inject(MAT_DIALOG_DATA) public data: AiSolutionInfoDialogData,
              private dialogRef: MatDialogRef<AiSolutionInfoDialogComponent>) {
    this.solution = data.solution;
    this.installedEntities = data.solution?.installedEntities || [];
  }

  close(): void {
    this.dialogRef.close();
  }

}
