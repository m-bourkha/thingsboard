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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { GroupPermissionService } from '@core/http/group-permission.service';
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup } from '@shared/models/entity-group.model';
import { Role, RoleType } from '@shared/models/role.model';
import { EntityType } from '@shared/models/entity-type.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { RoleId } from '@shared/models/id/role-id';
import {Router} from "@angular/router";

export interface AddGroupPermissionDialogData {
  entityGroup: EntityGroup;
}

@Component({
  selector: 'tb-add-group-permission-dialog',
  templateUrl: './add-group-permission-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AddGroupPermissionDialogComponent
  extends DialogComponent<AddGroupPermissionDialogComponent, boolean>
  implements OnInit {

  form: UntypedFormGroup;
  groupRoles: Role[] = [];
  userGroups: EntityGroup[] = [];
  noGroupRoles = false;

  constructor(
    protected store: Store<AppState>,
    public dialogRef: MatDialogRef<AddGroupPermissionDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: AddGroupPermissionDialogData,
    private fb: UntypedFormBuilder,
    private cd: ChangeDetectorRef,
    private groupPermissionService: GroupPermissionService,
    private roleService: RoleService,
    private entityGroupService: EntityGroupService,
    protected router: Router
  ) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      roleId: [null, Validators.required],
      userGroupId: [null, Validators.required]
    });

    const sortOrder: SortOrder = { property: 'name', direction: Direction.ASC };
    const pageLink = new PageLink(1024, 0, null, sortOrder);

    this.roleService.getRoles(undefined, pageLink).subscribe(page => {
      this.groupRoles = page.data.filter(r => r.type === RoleType.GROUP);
      this.noGroupRoles = this.groupRoles.length === 0;
      this.cd.markForCheck();
    });

    const entityGroup = this.data.entityGroup;
    const owner = entityGroup.ownerId;
    if (owner) {
      this.entityGroupService.getEntityGroupsByOwner(owner, EntityType.USER, pageLink).subscribe(page => {
        this.userGroups = page.data;
        this.cd.markForCheck();
      });
    }
  }

  add(): void {
    if (this.form.invalid) {
      return;
    }
    const { roleId, userGroupId } = this.form.value;
    const entityGroup = this.data.entityGroup;
    this.groupPermissionService.saveGroupPermission({
      userGroupId: new EntityGroupId(userGroupId),
      roleId: new RoleId(roleId),
      entityGroupId: new EntityGroupId(entityGroup.id.id),
      entityGroupType: entityGroup.type,
      publicPermission: false
    } as any).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
