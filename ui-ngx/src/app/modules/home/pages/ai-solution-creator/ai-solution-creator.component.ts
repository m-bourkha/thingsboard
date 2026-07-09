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

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatStepper } from '@angular/material/stepper';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { AiSolutionCreatorService } from '@core/http/ai-solution-creator.service';
import { AiModelService } from '@core/http/ai-model.service';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  AiSolution,
  AiSolutionInstallResult,
  AiSolutionSpec,
  solutionSuggestions,
  SolutionSuggestion
} from '@shared/models/ai-solution-creator.models';

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

  @ViewChild('stepper') stepper: MatStepper;

  readonly suggestions: SolutionSuggestion[] = solutionSuggestions;

  models: ModelOption[] = [];
  selectedModelId: string = null;

  savedSolutions: AiSolution[] = [];

  currentSolution: AiSolution = null;
  spec: AiSolutionSpec = null;
  advancedJson = '';
  viewMode: 'basic' | 'advanced' = 'basic';
  activeTab = 0;

  prompt = '';
  chatInput = '';
  messages: ChatMessage[] = [];

  generating = false;
  installing = false;
  installResult: AiSolutionInstallResult = null;

  constructor(private aiSolutionService: AiSolutionCreatorService,
              private aiModelService: AiModelService,
              private dialogService: DialogService,
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

  applySuggestion(suggestion: SolutionSuggestion): void {
    this.prompt = suggestion.prompt;
  }

  generate(): void {
    if (!this.selectedModelId || !this.prompt.trim()) {
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
    if (!message || !this.selectedModelId || !this.spec) {
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
    this.advancedJson = JSON.stringify(spec, null, 2);
  }

  applyAdvancedJson(): void {
    if (this.viewMode !== 'advanced' || !this.advancedJson) {
      return;
    }
    try {
      this.spec = JSON.parse(this.advancedJson);
    } catch (e) {
      console.error('Invalid solution JSON', e);
    }
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
    this.applyAdvancedJson();
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
          this.loadSolutions();
        },
        error: () => {
          this.installing = false;
        }
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
            this.loadSolutions();
          },
          error: () => {
            this.installing = false;
          }
        });
      }
    });
  }

  selectSolution(solution: AiSolution): void {
    this.aiSolutionService.getSolution(solution.id).subscribe(loaded => {
      this.currentSolution = loaded;
      this.prompt = loaded.originalPrompt || '';
      this.installResult = null;
      this.messages = [];
      if (loaded.originalPrompt) {
        this.messages.push({ role: 'user', text: loaded.originalPrompt });
      }
      this.applySpec(loaded.spec || {});
    });
  }

  newSolution(): void {
    this.currentSolution = null;
    this.spec = null;
    this.advancedJson = '';
    this.prompt = '';
    this.chatInput = '';
    this.messages = [];
    this.installResult = null;
    this.viewMode = 'basic';
  }

  deleteSolution(solution: AiSolution, event: MouseEvent): void {
    event.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('ai-solution-creator.delete'),
      this.translate.instant('ai-solution-creator.delete-confirm', { name: solution.name }),
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
