package soham.weaviate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.HybridArgument;
import io.weaviate.client.v1.graphql.query.argument.NearTextArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static soham.weaviate.ReadConfig.readProperty;

public class WeaviateIndexer {
    private static final Logger log = LoggerFactory.getLogger(WeaviateIndexer.class);
    private final WeaviateClient client;

    public WeaviateIndexer() throws IOException {
        Map<String, String> headers = new HashMap<>() { {
            put("X-Azure-Api-Key", readProperty("client.azureopenai.key"));
        } };

        Config config = new Config(readProperty("weaviate.url.protocol"),
                readProperty("weaviate.url.hostport"), headers);
        client = new WeaviateClient(config);
        checkSchemaAndIngestResumes();
    }

    private void checkSchemaAndIngestResumes() {
        Result<WeaviateClass> run = client.schema().classGetter().withClassName("CVs").run();
        if(run.getResult() == null) {
            try {
                cleanSchemas();
                createSchema();
                indexResumes();
            } catch (IOException e) {
                log.error("Error in creating schema and ingesting resumes", e);
            }
        }
        else {
            log.debug("Schema exists");
            Result<GraphQLResponse> count = client.graphQL().aggregate().withClassName("CVs")
                    .withFields(Field.builder().name("meta").fields(Field.builder().name("count").build()).build()).run();
            log.debug("Schema has {} CVs", count.getResult().getData());
            Map map = (Map) count.getResult().getData();
            Double sizeOfSchema = (Double)(((Map)((Map)((ArrayList)((Map)map.get("Aggregate")).get("CVs")).get(0)).get("meta")).get("count"));
            if(sizeOfSchema == 0.0) {
                log.debug("Schema has no resumes indexed");
                try {
                    indexResumes();
                } catch (IOException e) {
                    log.error("Error in ingesting resumes", e);
                }
            }
            else {
                GraphQLResponse result = client.graphQL().get().withClassName("CVs")
                        .withFields(Field.builder().name("title").build()
                                , Field.builder().name("filepath").build()
                                , Field.builder().name("contentVector").build()
                            , Field.builder().name("_additional").fields(Field.builder().name("vector").build()).build())
                        .withLimit(1)
                        .run().getResult();
                log.debug("Schema has {} data", result.getData());
            }
        }
    }

    private void cleanSchemas() {
        Result<Boolean> run = client.schema().allDeleter().run();
        log.debug("Schemas deletion result {}", run.getResult());
    }
    public String queryData(String query) {
        GraphQLResponse cVs = client.graphQL().get().withClassName("CVs")
                .withFields(
                        Field.builder().name("title").build(),
                        Field.builder().name("filepath").build(),
                        Field.builder().name("contentVector").build()
                       // , Field.builder().name("_additional").fields(Field.builder().name("score").build(), Field.builder().name("explainScore").build()).build()
                )
                //.withGenerativeSearch(GenerativeSearchBuilder.builder().singleResultPrompt("summarize each results with their core skills").build())
                //.withNearText(NearTextArgument.builder().concepts(new String[]{query}).build())
                .withHybrid(HybridArgument.builder().query(query).alpha(0.7f).build())
                .withLimit(3)
                .run().getResult();
        if(cVs.getErrors() != null && cVs.getErrors().length > 0) {
            log.error("CVs query error {}", cVs.getErrors()[0].getMessage());
        }
        log.debug("CVs query result {}", cVs.getData());
        return new Gson().toJson(((Map)((Map)cVs.getData()).get("Get")).get("CVs"));
    }

    public void indexResumes() throws IOException {
        List<Resume> resumes = readResumes();
        ObjectsBatcher batcher = client.batch().objectsAutoBatcher();

        for(Resume resume : resumes) {
            WeaviateObject object = WeaviateObject.builder()
                    .className("CVs")
                    .properties(Map.of("title", resume.getTitle(),
                            "filepath", resume.getFilepath(),
                            "contentVector", resume.getContentVector()))
                    .build();

            batcher.withObject(object);
        }
        batcher.flush();
        batcher.close();
    }

    private void createSchema() throws IOException {
        WeaviateClass clazz = WeaviateClass.builder()
                .moduleConfig(createModuleConfig())
                .className("CVs")
                .description("collection of CVs")
                .vectorizer("text2vec-openai")
                .properties(List.of(
                        Property.builder().name("title").dataType(List.of("text"))
                                .moduleConfig(createPropModuleConfig(true))
                                .build(),
                        Property.builder().name("filepath").dataType(List.of("text"))
                                .moduleConfig(createPropModuleConfig(true))
                                .build(),
                        Property.builder().name("contentVector").dataType(List.of("text"))
                                .moduleConfig(createPropModuleConfig(false))
                                .build()
                ))
                .build();

        log.debug("Schema {}", new GsonBuilder().create().toJson(clazz));

        Result<Boolean> run = client.schema().classCreator().withClass(clazz).run();

        log.debug("Schema creation result {}", run.getResult());
    }

    private Map<String, Object> createModuleConfig() throws IOException {
        Map<String, Object> text2vec = new HashMap<>();
        text2vec.put("resourceName", readProperty("client.azureopenai.resourceName"));
        text2vec.put("deploymentId", readProperty("client.azureopenai.deploymentId"));

        Map<String, Object> moduleConfig = new HashMap<>();
        moduleConfig.put("text2vec-openai", text2vec);

        return moduleConfig;
    }

    private Map<String, Object> createPropModuleConfig(boolean skip) {
        Map<String, Object> propModule = new HashMap<>();
        propModule.put("skip", skip);

        Map<String, Object> moduleConfig = new HashMap<>();
        moduleConfig.put("text2vec-openai", propModule);

        return moduleConfig;
    }

    private List<Resume> readResumes() throws IOException {
        List<Resume> resumes = new ArrayList<>();
        File folder = new File("C:\\Users\\sohadasgupta\\IdeaProjects\\Weaviate\\resumes\\CONSULTANT");
        for(File file : folder.listFiles()) {
            if(file.isFile()) {
                PDDocument document = PDDocument.load(file);
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                Resume resume = new Resume(file.getName(), file.getAbsolutePath(), text);
                document.close();

                resumes.add(resume);
            }
        }
        return resumes;
    }
}
