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

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSelectionListChange } from '@angular/material/list';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { AiSolutionCreatorService } from '@core/http/ai-solution-creator.service';
import { AiModelService } from '@core/http/ai-model.service';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { deepClone } from '@core/utils';
import {
  AiSolutionInfoDialogComponent,
  AiSolutionInfoDialogData
} from '@home/pages/ai-solution-creator/ai-solution-info-dialog.component';
import {
  AiSolutionRenameDialogComponent,
  AiSolutionRenameDialogData
} from '@home/pages/ai-solution-creator/ai-solution-rename-dialog.component';
import {
  AiSolution,
  AiSolutionInstallResult,
  AiSolutionSpec,
  DashboardSpec,
  solutionSuggestions,
  SolutionSuggestion
} from '@shared/models/ai-solution-creator.models';

/** Wizard steps, in the order they appear in the stepper header. */
const ARCHITECTURE_STEP = 0;
const DASHBOARD_STEP = 1;
const INSTALL_STEP = 2;

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
}

interface ModelOption {
  id: string;
  name: string;
}

@Component({
  selector: 'tb-ai-solution-creator',
  templateUrl: './ai-solution-creator.component.html',
  styleUrls: ['./ai-solution-creator.component.scss'],
  standalone: false
})
export class AiSolutionCreatorComponent extends PageComponent implements OnInit {

  readonly architectureStep = ARCHITECTURE_STEP;
  readonly dashboardStep = DASHBOARD_STEP;
  readonly installStep = INSTALL_STEP;

  readonly steps = [
    'ai-solution-creator.step-architecture',
    'ai-solution-creator.step-dashboards',
    'ai-solution-creator.step-install'
  ];

  readonly suggestions: SolutionSuggestion[] = solutionSuggestions;

  currentStep = ARCHITECTURE_STEP;
  sideBarExpanded = true;

  models: ModelOption[] = [];
  selectedModelId: string = null;

  savedSolutions: AiSolution[] = [];

  currentSolution: AiSolution = null;
  spec: AiSolutionSpec = null;
  viewMode: 'basic' | 'advanced' = 'basic';
  activeTab = 0;

  /**
   * Working copies edited in the Advanced (JSON) view. `tb-json-object-edit` pushes a new object on every
   * keystroke that parses, so the edits are staged here and only merged into `spec` when Apply is pressed.
   */
  specModel: AiSolutionSpec = null;
  specModelDirty = false;
  dashboardsModel: DashboardSpec[] = null;
  dashboardsModelDirty = false;

  dashboards: DashboardSpec[] = null;
  selectedDashboardIndex = 0;
  generatingDashboards = false;

  prompt = '';
  chatInput = '';
  messages: ChatMessage[] = [];

  generating = false;
  installing = false;
  installResult: AiSolutionInstallResult = null;

  constructor(private aiSolutionService: AiSolutionCreatorService,
              private aiModelService: AiModelService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private router: Router,
              private translate: TranslateService) {
    super();
  }

  ngOnInit(): void {
    this.loadModels();
    this.loadSolutions();
  }

  private loadModels(): void {
    const pageLink = new PageLink(100, 0, null, { property: 'name', direction: Direction.ASC });
    this.aiModelService.getAiModels(pageLink, { ignoreLoading: true }).subscribe(page => {
      this.models = page.data.map(m => ({ id: m.id.id, name: m.name }));
      if (this.models.length && !this.selectedModelId) {
        this.selectedModelId = this.models[0].id;
      }
    });
  }

  private loadSolutions(): void {
    const pageLink = new PageLink(100, 0, null, { property: 'createdTime', direction: Direction.DESC });
    this.aiSolutionService.getSolutions(pageLink, { ignoreLoading: true }).subscribe(page => {
      this.savedSolutions = page.data;
    });
  }

  get hasSpec(): boolean {
    return !!this.spec;
  }

  /**
   * A fully installed solution replaces the whole wizard body with the summary screen: its entities
   * already exist, so refining the spec in place would silently diverge from what is deployed.
   * A partial install keeps the normal step-3 layout, where the result table explains what failed.
   */
  get showInstalledScreen(): boolean {
    return this.currentStep === INSTALL_STEP && this.currentSolution?.status === 'INSTALLED';
  }

  /** The first dashboard the install created, if any — the target of the 'Main dashboard' button. */
  get mainDashboardId(): string {
    return this.currentSolution?.installedEntities?.find(entity => entity.entityType === 'DASHBOARD')?.id;
  }

  get overviewTitle(): string {
    const name = this.spec?.name;
    switch (this.currentStep) {
      case DASHBOARD_STEP:
        return this.translate.instant('ai-solution-creator.dashboards-overview', { name });
      case INSTALL_STEP:
        return this.translate.instant('ai-solution-creator.installation-overview', { name });
      default:
        return this.translate.instant('ai-solution-creator.architecture-overview', { name });
    }
  }

  /** Enter submits, Shift+Enter inserts a newline. Returns whether the caller should submit. */
  submitOnEnter(event: KeyboardEvent): boolean {
    if (event.shiftKey) {
      return false;
    }
    event.preventDefault();
    return true;
  }

  applySuggestion(suggestion: SolutionSuggestion): void {
    this.prompt = suggestion.prompt;
  }

  generate(): void {
    if (this.generating || !this.selectedModelId || !this.prompt.trim()) {
      return;
    }
    this.generating = true;
    this.messages = [{ role: 'user', text: this.prompt }];
    this.aiSolutionService.generateArchitecture({ modelId: this.selectedModelId, prompt: this.prompt })
      .subscribe({
        next: (spec) => {
          this.applySpec(spec);
          this.messages.push({ role: 'assistant', text: spec.description || this.translate.instant('ai-solution-creator.generated') });
          this.currentSolution = null;
          this.currentStep = ARCHITECTURE_STEP;
          this.saveDraft();
          this.generating = false;
        },
        error: () => {
          this.generating = false;
        }
      });
  }

  sendRefinement(): void {
    const message = this.chatInput.trim();
    if (this.generating || !message || !this.selectedModelId || !this.spec) {
      return;
    }
    this.chatInput = '';
    this.messages.push({ role: 'user', text: message });
    this.generating = true;
    this.aiSolutionService.refineArchitecture({ modelId: this.selectedModelId, currentSpec: this.spec, message })
      .subscribe({
        next: (spec) => {
          this.applySpec(spec);
          this.messages.push({ role: 'assistant', text: this.translate.instant('ai-solution-creator.updated') });
          this.saveDraft();
          this.generating = false;
        },
        error: () => {
          this.generating = false;
        }
      });
  }

  private applySpec(spec: AiSolutionSpec): void {
    this.spec = spec;
    this.specModel = deepClone(spec);
    this.specModelDirty = false;
    this.readDashboardsFromSpec();
  }

  /**
   * The dashboards live inside the spec, so that they persist with it. Re-generating or refining the
   * architecture drops them (the architecture prompt never emits a `dashboards` block), which is what
   * sends the user back through the Dashboard Design pass.
   */
  private readDashboardsFromSpec(): void {
    this.dashboards = this.spec?.dashboards?.length ? this.spec.dashboards : null;
    this.dashboardsModel = this.dashboards ? deepClone(this.dashboards) : null;
    this.dashboardsModelDirty = false;
    this.selectedDashboardIndex = 0;
  }

  /**
   * Assign the emitted reference as-is: `ngModel` skips `writeValue` when the model is identical to the
   * view model, and a fresh object here would reset the Ace editor (and the caret) on every keystroke.
   * Invalid JSON emits `null`, which keeps Apply disabled without disturbing what the user typed.
   */
  onSpecModelChange(spec: AiSolutionSpec): void {
    this.specModel = spec;
    this.specModelDirty = !!spec;
  }

  applySpecModel(): void {
    if (!this.specModel) {
      return;
    }
    this.applySpec(deepClone(this.specModel));
    this.saveDraft();
  }

  onDashboardsModelChange(dashboards: DashboardSpec[]): void {
    this.dashboardsModel = dashboards;
    this.dashboardsModelDirty = Array.isArray(dashboards);
  }

  applyDashboardsModel(): void {
    if (!Array.isArray(this.dashboardsModel)) {
      return;
    }
    this.setDashboards(deepClone(this.dashboardsModel));
    // Persist right away: 'Create solution' installs the saved spec, not the in-memory one.
    this.saveDraft();
  }

  nextStep(): void {
    if (this.currentStep >= INSTALL_STEP) {
      return;
    }
    this.currentStep++;
    this.viewMode = 'basic';
    if (this.currentStep === DASHBOARD_STEP && !this.dashboards && !this.generatingDashboards) {
      this.generateDashboards();
    }
  }

  previousStep(): void {
    if (this.currentStep > ARCHITECTURE_STEP) {
      this.currentStep--;
      this.viewMode = 'basic';
    }
  }

  get selectedDashboard(): DashboardSpec {
    return this.dashboards ? this.dashboards[this.selectedDashboardIndex] : null;
  }

  onDashboardSelected(event: MatSelectionListChange): void {
    const option = event.options[0];
    if (option?.selected) {
      this.selectedDashboardIndex = option.value;
    }
  }

  generateDashboards(): void {
    if (!this.selectedModelId || !this.spec) {
      return;
    }
    this.generatingDashboards = true;
    this.aiSolutionService.generateDashboards({ modelId: this.selectedModelId, spec: this.spec })
      .subscribe({
        next: (dashboards) => {
          this.setDashboards(dashboards);
          this.messages.push({
            role: 'assistant',
            text: this.translate.instant('ai-solution-creator.dashboards-generated', { count: dashboards.length })
          });
          this.generatingDashboards = false;
          this.saveDraft();
        },
        error: () => {
          this.generatingDashboards = false;
        }
      });
  }

  private setDashboards(dashboards: DashboardSpec[]): void {
    this.dashboards = dashboards;
    this.spec.dashboards = dashboards;
    this.specModel = deepClone(this.spec);
    this.specModelDirty = false;
    this.dashboardsModel = deepClone(dashboards);
    this.dashboardsModelDirty = false;
    this.selectedDashboardIndex = 0;
  }

  private saveDraft(): void {
    const solution: AiSolution = {
      id: this.currentSolution ? this.currentSolution.id : undefined,
      version: this.currentSolution ? this.currentSolution.version : undefined,
      name: this.spec?.name || this.translate.instant('ai-solution-creator.untitled'),
      originalPrompt: this.prompt,
      status: this.currentSolution?.status || 'DRAFT',
      spec: this.spec
    };
    this.aiSolutionService.saveSolution(solution, { ignoreLoading: true }).subscribe(saved => {
      this.currentSolution = saved;
      this.loadSolutions();
    });
  }

  saveSolution(): void {
    this.saveDraft();
  }

  install(): void {
    if (!this.currentSolution) {
      return;
    }
    this.installing = true;
    this.installResult = null;
    this.aiSolutionService.installSolution(this.currentSolution.id)
      .subscribe({
        next: (result) => {
          this.installResult = result;
          this.installing = false;
          this.currentSolution.status = result.success ? 'INSTALLED' : 'PARTIALLY_INSTALLED';
          // Install only returns the per-item report; re-read the solution to pick up installedEntities,
          // which back the 'Main dashboard' and 'Info' actions on the summary screen.
          this.reloadCurrentSolution();
          this.loadSolutions();
        },
        error: () => {
          this.installing = false;
        }
      });
  }

  private reloadCurrentSolution(): void {
    this.aiSolutionService.getSolution(this.currentSolution.id, { ignoreLoading: true })
      .subscribe(loaded => this.currentSolution = loaded);
  }

  editSolution(): void {
    this.currentStep = ARCHITECTURE_STEP;
    this.viewMode = 'basic';
  }

  openMainDashboard(): void {
    const dashboardId = this.mainDashboardId;
    if (dashboardId) {
      this.router.navigateByUrl(`/dashboards/${dashboardId}`);
    }
  }

  openInfo(): void {
    this.dialog.open<AiSolutionInfoDialogComponent, AiSolutionInfoDialogData>(AiSolutionInfoDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: { solution: this.currentSolution }
    });
  }

  uninstall(): void {
    if (!this.currentSolution) {
      return;
    }
    this.dialogService.confirm(
      this.translate.instant('ai-solution-creator.uninstall'),
      this.translate.instant('ai-solution-creator.uninstall-confirm'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.installing = true;
        this.aiSolutionService.uninstallSolution(this.currentSolution.id).subscribe({
          next: (result) => {
            this.installResult = result;
            this.installing = false;
            this.currentSolution.status = 'UNINSTALLED';
            this.reloadCurrentSolution();
            this.loadSolutions();
          },
          error: () => {
            this.installing = false;
          }
        });
      }
    });
  }

  /** @param edit open on the Architecture step even when the solution is installed. */
  selectSolution(solution: AiSolution, edit = false): void {
    this.aiSolutionService.getSolution(solution.id).subscribe(loaded => {
      this.currentSolution = loaded;
      this.prompt = loaded.originalPrompt || '';
      this.installResult = null;
      this.messages = [];
      this.viewMode = 'basic';
      if (loaded.originalPrompt) {
        this.messages.push({ role: 'user', text: loaded.originalPrompt });
      }
      this.applySpec(loaded.spec || {});
      // An installed solution opens straight on its summary screen rather than back at step 1,
      // unless the user explicitly asked to edit it.
      this.currentStep = !edit && loaded.status === 'INSTALLED' ? INSTALL_STEP : ARCHITECTURE_STEP;
    });
  }

  newSolution(): void {
    this.currentSolution = null;
    this.spec = null;
    this.specModel = null;
    this.specModelDirty = false;
    this.prompt = '';
    this.chatInput = '';
    this.messages = [];
    this.installResult = null;
    this.viewMode = 'basic';
    this.currentStep = ARCHITECTURE_STEP;
    this.dashboards = null;
    this.dashboardsModel = null;
    this.dashboardsModelDirty = false;
    this.selectedDashboardIndex = 0;
  }

  openForEdit(solution: AiSolution, event: MouseEvent): void {
    event.stopPropagation();
    this.selectSolution(solution, true);
  }

  renameSolution(solution: AiSolution, event: MouseEvent): void {
    event.stopPropagation();
    this.dialog.open<AiSolutionRenameDialogComponent, AiSolutionRenameDialogData, string>(
      AiSolutionRenameDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { name: solution.name }
      }).afterClosed().subscribe(name => {
        if (!name || name === solution.name) {
          return;
        }
        // Re-read: the sidebar rows are list projections, and saving needs the full spec back.
        this.aiSolutionService.getSolution(solution.id).subscribe(loaded => {
          loaded.name = name;
          if (loaded.spec) {
            // saveDraft() derives the solution name from spec.name, so a record-only rename
            // would be silently reverted by the next architecture save.
            loaded.spec.name = name;
          }
          this.aiSolutionService.saveSolution(loaded).subscribe(saved => {
            if (this.currentSolution?.id === saved.id) {
              this.currentSolution = saved;
              if (this.spec) {
                this.spec.name = name;
                this.specModel = deepClone(this.spec);
                this.specModelDirty = false;
              }
            }
            this.loadSolutions();
          });
        });
      });
  }

  deleteSolution(solution: AiSolution, event: MouseEvent): void {
    event.stopPropagation();
    // Deleting an installed solution cascades to every entity the install created, so say so up front.
    const installed = solution.status === 'INSTALLED' || solution.status === 'PARTIALLY_INSTALLED';
    const message = installed
      ? this.translate.instant('ai-solution-creator.delete-installed-confirm', { name: solution.name })
      : this.translate.instant('ai-solution-creator.delete-confirm', { name: solution.name });
    this.dialogService.confirm(
      this.translate.instant('ai-solution-creator.delete'),
      message,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.aiSolutionService.deleteSolution(solution.id).subscribe(() => {
          if (this.currentSolution && this.currentSolution.id === solution.id) {
            this.newSolution();
          }
          this.loadSolutions();
        });
      }
    });
  }

  isInstalled(): boolean {
    return this.currentSolution?.status === 'INSTALLED' || this.currentSolution?.status === 'PARTIALLY_INSTALLED';
  }

  keyList(telemetry: { key?: string }[]): string {
    if (!telemetry || !telemetry.length) {
      return '—';
    }
    const keys = telemetry.map(t => t.key).filter(k => !!k);
    const shown = keys.slice(0, 6).join(', ');
    return keys.length > 6 ? `${shown} +${keys.length - 6}` : shown;
  }

}
