package soham.weaviate;

import com.microsoft.semantickernel.exceptions.ConfigurationException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, ConfigurationException {
        WeaviateIndexer weaviateIndexer = new WeaviateIndexer();

        String queryData = weaviateIndexer.queryData("J2EE");

        SKComponent skComponent = new SKComponent();
        //skComponent.invokeKernelWithRelevantMemory(queryData, "2 years experience in Spring");
    }
}