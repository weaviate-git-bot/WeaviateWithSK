# Weaviate with Semantic Kernel

Code example of indexing/vectorizing Resumes/CVs in Weavaite using Azure OpenAI Embeddings.
Then using Semantic Kernel to summarize the search results from Weaviate.

## Pre reqs to run the samples

1. Create [Azure Open AI](https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/create-resource?pivots=web-portal) resource in Azure portal and deploy 'gpt-X' and 'text-embedding-ada-002' models there.
2. Rename ````src/main/resources/conf.properties.example```` to ````src/main/resources/conf.properties```` and update it with the details from Step #1.

## Sample

1. Run ```` docker-compose up -d ```` to start Weaviate locally.
2. Run ```` mvn clean compile ```` to build the project.
3. Run the Main class (with extra JVM args  `--add-opens=java.base/java.lang=ALL-UNNAMED`  if JVM > 9)

## What it is doing

1. Upon start, the `WeaviateIndexer` class will index the sample resumes in the ````data```` folder into Weaviate. This is done once, since each time it checks whether the schema is available and also some data. 
2. Then it will search for the term ````J2EE```` and convert the results in JSON format. 
3. It then passes that JSON as relevant context to Semantic Kernel to list the resume details with top X skills relevant for the initial search term.

## Documentation/Further reading

1. [Semantic Kernel Official Docs](https://learn.microsoft.com/en-us/semantic-kernel/overview/)
2. [Semantic Kernel Github Repo](https://github.com/microsoft/semantic-kernel)
3. [Weaviate Docs](https://weaviate.io/developers/weaviate)
