package soham.weaviate;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.util.AzureOpenAISettings;
import com.microsoft.semantickernel.connectors.ai.openai.util.SettingsMap;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.skilldefinition.ReadOnlyFunctionCollection;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;

public class SKComponent {
    private static final Logger log = LoggerFactory.getLogger(SKComponent.class);

    private final CompletionSKFunction extractResume;
    public SKComponent() throws ConfigurationException {
        log.debug("== Instantiates semantic kernel with resume plugin ==");
        Kernel kernel = initKernel();
        ReadOnlyFunctionCollection skill = kernel.importSkillFromDirectory("Resume", "src/main/resources/plugins", "Resume");
        extractResume = skill.getFunction("ExtractDetails", CompletionSKFunction.class);
    }
    private Kernel initKernel() throws ConfigurationException {
        AzureOpenAISettings settings = new AzureOpenAISettings(SettingsMap.
                getWithAdditional(List.of(
                        new File("src/main/resources/conf.properties"))));
        OpenAIAsyncClient client = new OpenAIClientBuilder().endpoint(settings.getEndpoint()).credential(new AzureKeyCredential(settings.getKey())).buildAsyncClient();

        TextCompletion textCompletion = SKBuilders.chatCompletion()
                .withOpenAIClient(client)
                .withModelId(settings.getDeploymentName())
                .build();
        return SKBuilders.kernel().withDefaultAIService(textCompletion).build();
    }

    public void invokeKernelWithRelevantMemory(String relevantMemory, String originalQuery) {

        log.debug("== Set Resume extractor variables ==");
        SKContext resumeContext = SKBuilders.context().build();
        resumeContext.setVariable("requirement", originalQuery);
        resumeContext.setVariable("resume", relevantMemory);

        log.debug("== Run Resume extractor ==");
        Mono<SKContext> summary = extractResume.invokeAsync(resumeContext);

        log.debug("== Result ==");
        log.debug(summary.block().getResult());
    }
}
