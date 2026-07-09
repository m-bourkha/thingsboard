/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ai.solution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelType;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.DashboardSpec;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.AiChatModelService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultAiSolutionGenerationService implements AiSolutionGenerationService {

    private final AiModelService aiModelService;
    private final AiChatModelService aiChatModelService;

    @Override
    public ListenableFuture<AiSolutionSpec> generate(TenantId tenantId, AiModelId modelId, String prompt) {
        return sendAndParse(tenantId, modelId, AiSolutionPrompts.SYSTEM_PROMPT,
                AiSolutionPrompts.generateUserPrompt(prompt), DefaultAiSolutionGenerationService::parseSpec);
    }

    @Override
    public ListenableFuture<AiSolutionSpec> refine(TenantId tenantId, AiModelId modelId, AiSolutionSpec currentSpec, String message) {
        String currentSpecJson = JacksonUtil.toString(currentSpec);
        return sendAndParse(tenantId, modelId, AiSolutionPrompts.SYSTEM_PROMPT,
                AiSolutionPrompts.refineUserPrompt(currentSpecJson, message), DefaultAiSolutionGenerationService::parseSpec);
    }

    @Override
    public ListenableFuture<List<DashboardSpec>> generateDashboards(TenantId tenantId, AiModelId modelId, AiSolutionSpec spec) {
        String specJson = JacksonUtil.toString(spec);
        return sendAndParse(tenantId, modelId, AiSolutionPrompts.DASHBOARD_SYSTEM_PROMPT,
                AiSolutionPrompts.dashboardUserPrompt(specJson), DefaultAiSolutionGenerationService::parseDashboards);
    }

    private <T> ListenableFuture<T> sendAndParse(TenantId tenantId, AiModelId modelId, String systemPrompt,
                                                 String userPrompt, Function<String, T> parser) {
        return aiModelService.findAiModelByTenantIdAndIdAsync(tenantId, modelId)
                .transformAsync(modelOpt -> {
                    AiModel model = modelOpt.orElseThrow(() ->
                            new NoSuchElementException("AI model with id [" + modelId + "] was not found"));
                    return sendChatRequest(model, systemPrompt, userPrompt);
                }, directExecutor())
                .transform(chatResponse -> parser.apply(chatResponse.aiMessage().text()), directExecutor());
    }

    private <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequest(AiModel model, String systemPrompt, String userPrompt) {
        AiModelType modelType = model.getConfiguration().modelType();
        if (modelType != AiModelType.CHAT) {
            throw new IllegalStateException("AI model [" + model.getId() + "] must be of type CHAT, but was " + modelType);
        }

        @SuppressWarnings("unchecked")
        AiChatModelConfig<C> config = (AiChatModelConfig<C>) model.getConfiguration();

        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(List.<dev.langchain4j.data.message.ChatMessage>of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)));

        if (config.supportsJsonMode()) {
            requestBuilder.responseFormat(ResponseFormat.JSON);
        }

        return aiChatModelService.sendChatRequestAsync(config, requestBuilder.build());
    }

    private static AiSolutionSpec parseSpec(String responseText) {
        String json = extractJsonObject(responseText);
        try {
            AiSolutionSpec spec = JacksonUtil.fromString(json, AiSolutionSpec.class);
            if (spec == null) {
                throw new IllegalArgumentException("AI response did not contain a JSON object");
            }
            return spec;
        } catch (Exception e) {
            log.debug("Failed to parse AI solution spec from response: {}", responseText, e);
            throw new IllegalArgumentException("Failed to parse AI response as a solution specification: " + e.getMessage());
        }
    }

    private static List<DashboardSpec> parseDashboards(String responseText) {
        String json = extractJsonArray(responseText);
        try {
            List<DashboardSpec> dashboards = JacksonUtil.fromString(json, new TypeReference<List<DashboardSpec>>() {});
            if (dashboards == null) {
                throw new IllegalArgumentException("AI response did not contain a JSON array");
            }
            return dashboards;
        } catch (Exception e) {
            log.debug("Failed to parse AI dashboard specs from response: {}", responseText, e);
            throw new IllegalArgumentException("Failed to parse AI response as a dashboard specification: " + e.getMessage());
        }
    }

    /**
     * Best-effort extraction of the JSON object from a model response that may be wrapped in
     * markdown fences or surrounded by prose.
     */
    static String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        String trimmed = stripMarkdownFences(text);
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * Best-effort extraction of the JSON array from a model response. Slicing from the first '[' to the
     * last ']' also transparently unwraps a {@code {"dashboards": [...]}} envelope, which is what models
     * running in strict JSON mode (which requires a top-level object) tend to return.
     */
    static String extractJsonArray(String text) {
        if (text == null || text.isBlank()) {
            return "[]";
        }
        String trimmed = stripMarkdownFences(text);
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private static String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

}
