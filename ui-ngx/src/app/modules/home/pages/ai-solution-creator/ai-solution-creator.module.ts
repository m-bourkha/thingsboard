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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TextFieldModule } from '@angular/cdk/text-field';
import { SharedModule } from '@shared/shared.module';
import { AiSolutionCreatorRoutingModule } from '@home/pages/ai-solution-creator/ai-solution-creator-routing.module';
import { AiSolutionCreatorComponent } from '@home/pages/ai-solution-creator/ai-solution-creator.component';
import { AiSolutionInfoDialogComponent } from '@home/pages/ai-solution-creator/ai-solution-info-dialog.component';
import { AiSolutionRenameDialogComponent } from '@home/pages/ai-solution-creator/ai-solution-rename-dialog.component';

@NgModule({
  declarations: [
    AiSolutionCreatorComponent,
    AiSolutionInfoDialogComponent,
    AiSolutionRenameDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    TextFieldModule,
    AiSolutionCreatorRoutingModule
  ]
})
export class AiSolutionCreatorModule { }
