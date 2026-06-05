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

import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroup } from '@shared/models/entity-group.model';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import {AsyncPipe} from "@angular/common";

@Component({
  selector: 'tb-entity-groups-tab',

  standalone: false,
  template: `
    <div class="tb-entity-groups-tab">
      <div *ngFor="let group of (groups$ | async)?.data"
           class="tb-entity-group-row"
           (click)="onOpenGroup(group)">
        <span class="tb-group-name">{{ group.name }}</span>
        <span class="tb-group-type">{{ group.type }}</span>
        <span class="tb-group-public" *ngIf="group.publicGroup">Public</span>
      </div>
    </div>
  `
})
export class EntityGroupsTabComponent implements OnInit {

  @Input() entityType: EntityType;
  @Input() ownerId: EntityId;

  groups$: Observable<PageData<EntityGroup>>;

  private pageLink = new PageLink(1000);

  constructor(
    private entityGroupService: EntityGroupService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.entityType) {
      this.entityType = this.route.snapshot.data.entityType as EntityType;
    }
    if (this.ownerId) {
      this.groups$ = this.entityGroupService.getEntityGroupsByOwner(this.ownerId, this.entityType, this.pageLink);
    } else {
      this.groups$ = this.entityGroupService.getEntityGroups(this.entityType, this.pageLink);
    }
  }

  onOpenGroup(group: EntityGroup): void {
    this.router.navigate([group.id.id, 'entities'], { relativeTo: this.route });
  }
}
