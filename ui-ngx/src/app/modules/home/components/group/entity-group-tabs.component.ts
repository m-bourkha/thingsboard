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

import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { EntityGroup } from '@shared/models/entity-group.model';

@Component({
  selector: 'tb-entity-group-tabs',
  templateUrl: './entity-group-tabs.component.html',
  standalone: false,
  styleUrls: []
})
export class EntityGroupTabsComponent extends EntityTabsComponent<EntityGroup> {

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
    this.cd.markForCheck();
  }

  get isEdit(): boolean {
    return this.isEditValue;
  }

  get configurationForm(): UntypedFormGroup {
    return this.detailsForm?.get('configuration') as UntypedFormGroup;
  }

  get columnsControl(): UntypedFormControl {
    return this.configurationForm?.get('columns') as UntypedFormControl;
  }

  get settingsForm(): UntypedFormGroup {
    return this.configurationForm?.get('settings') as UntypedFormGroup;
  }

  get actionsControl(): UntypedFormControl {
    return this.configurationForm?.get('actions') as UntypedFormControl;
  }
}
