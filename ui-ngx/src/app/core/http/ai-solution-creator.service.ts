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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  AiSolution,
  AiSolutionGenerateRequest,
  AiSolutionInstallResult,
  AiSolutionRefineRequest,
  AiSolutionSpec
} from '@shared/models/ai-solution-creator.models';

@Injectable({
  providedIn: 'root'
})
export class AiSolutionCreatorService {

  constructor(
    private http: HttpClient
  ) {}

  public generateArchitecture(request: AiSolutionGenerateRequest, config?: RequestConfig): Observable<AiSolutionSpec> {
    return this.http.post<AiSolutionSpec>('/api/ai/solution/generate', request, defaultHttpOptionsFromConfig(config));
  }

  public refineArchitecture(request: AiSolutionRefineRequest, config?: RequestConfig): Observable<AiSolutionSpec> {
    return this.http.post<AiSolutionSpec>('/api/ai/solution/refine', request, defaultHttpOptionsFromConfig(config));
  }

  public saveSolution(solution: AiSolution, config?: RequestConfig): Observable<AiSolution> {
    return this.http.post<AiSolution>('/api/ai/solution', solution, defaultHttpOptionsFromConfig(config));
  }

  public getSolutions(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AiSolution>> {
    return this.http.get<PageData<AiSolution>>(`/api/ai/solution${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getSolution(solutionId: string, config?: RequestConfig): Observable<AiSolution> {
    return this.http.get<AiSolution>(`/api/ai/solution/${solutionId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteSolution(solutionId: string, config?: RequestConfig): Observable<Object> {
    return this.http.delete(`/api/ai/solution/${solutionId}`, defaultHttpOptionsFromConfig(config));
  }

  public installSolution(solutionId: string, config?: RequestConfig): Observable<AiSolutionInstallResult> {
    return this.http.post<AiSolutionInstallResult>(`/api/ai/solution/${solutionId}/install`, null, defaultHttpOptionsFromConfig(config));
  }

  public uninstallSolution(solutionId: string, config?: RequestConfig): Observable<AiSolutionInstallResult> {
    return this.http.post<AiSolutionInstallResult>(`/api/ai/solution/${solutionId}/uninstall`, null, defaultHttpOptionsFromConfig(config));
  }

}
