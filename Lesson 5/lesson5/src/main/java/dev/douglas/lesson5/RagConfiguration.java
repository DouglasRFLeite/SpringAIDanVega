package dev.douglas.lesson5;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class RagConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Bean
    SimpleVectorStore vectorStore() throws IOException {
        Resource[] resources = getDataFiles();
        getDocuments(resources);
        return null;
    }

    private Resource[] getDataFiles() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources("classpath:/data/*.txt");
        logger.info("Loaded Data Files: " + Arrays.stream(resources).map(Resource::getFilename).toList());
        return resources;
    }

    private List<Document> getDocuments(Resource[] resources) {
        List<Document> documents = Arrays.stream(resources).map(this::getDocFromResource).toList();
        TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(300).build();
        List<Document> splitDocuments = splitter.apply(documents);
        logger.info("Number of split Documents: " + splitDocuments.size());

        return splitDocuments;
    }

    private Document getDocFromResource(Resource resource) {
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        return textReader.read().getFirst();
    }

}