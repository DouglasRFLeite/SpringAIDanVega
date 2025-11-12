package dev.douglas.lesson5;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class RagConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);
    
    @Value("${rag.vectorstore}")
    private String vectorStoreFilename = "vectorstore.json";

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) throws IOException {

        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        File vectorStoreFile = Paths.get(vectorStoreFilename).toFile();
        if(vectorStoreFile.exists()){
            logger.info("Vector Store file exists");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            logger.info("Vector Store file does not exist, embedding documents");
            List<Document> documents = getDocuments();
            simpleVectorStore.add(documents);
            simpleVectorStore.save(vectorStoreFile);
        }

        return simpleVectorStore;
    }

    private List<Document> getDocuments() throws IOException {
        List<Document> documents = Arrays.stream(getDataFiles()).map(this::getDocFromResource).toList();
        TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(300).build();
        List<Document> splitDocuments = splitter.apply(documents);
        logger.info("Number of split Documents: " + splitDocuments.size());

        return splitDocuments;
    }

    private Resource[] getDataFiles() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources("classpath:/data/*.txt");
        logger.info("Loaded Data Files: " + Arrays.stream(resources).map(Resource::getFilename).toList());
        return resources;
    }

    private Document getDocFromResource(Resource resource) {
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        return textReader.read().getFirst();
    }

}