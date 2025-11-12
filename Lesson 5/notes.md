## Lesson 5

In this Lesson we'll take a more practical look into RAGs with Spring AI. I'll assume you already know what you need to get a Spring AI project up and running so let's get started.

The only new thing you'll need is to add the **Spring AI Vector Store** dependency to your application. In the future, we'll use vendor-specific dependecies, but not today. Just add this to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store</artifactId>
</dependency>
```

That said, I wanted to make a disclaimer that, since we'll move pretty slowly, it make take some time before we're able to actually run our code and see cool stuff happening.

### Documents

First thing we need to create a RAG is Documents. One good thing to do with RAGs is to split big documents into smaller chunks that behave as separate documents, so we don't have to, say, stuff the whole first chapter of John into our prompt if we just need the first few verses.

That said, we'll use two big documents: the first and second chapters of John. But we will split both of them into smaller chunks of text.

We we'll do a lot of the next couple of things inside our `RagConfiguration` file, so let's create it. We'll use a Logger just to see results more easly.

```java
@Configuration
public class RagConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    public Resource[] getDataFiles() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources("classpath:/data/*.txt");
        logger.info("Loaded Data Files: " + Arrays.stream(resources).map(Resource::getFilename).toList());
        return resources;
    }

    @Bean
    SimpleVectorStore vectorStore() throws IOException {
        getDataFiles();
        return null;
    }
}
```

Don't worry about that **SimpleVectorStore** just yet. I just created a Bean so we could see this in the logs:

> Loaded Data Files: [john1.txt, john2.txt]

Now we need to read those files and split them into Documents. To do that, let us first turn them into Documents that can be split:

```java
private Document getDocFromResource(Resource resource) {
    TextReader textReader = new TextReader(resource);
    textReader.getCustomMetadata().put("filename", resource.getFilename());
    return textReader.read().getFirst();
}
```

And we can use something called `TokenTextSplitter` to split our Documents into smaller ones based on the number of tokens. We can, if we want, set a different number of maximum tokens per chunk.

```java
private List<Document> getDocuments(Resource[] resources) {
    List<Document> documents = Arrays.stream(resources).map(this::getDocFromResource).toList();
    TokenTextSplitter splitter = new TokenTextSplitter();
    List<Document> splitDocuments = splitter.apply(documents);
    logger.info("Number of split Documents: " + splitDocuments.size());

    return splitDocuments;
}
```

Due to our documents not being too big, this will split only the first one into two and won't even touch the second. So we're going to change the token size.

```java
private List<Document> getDocuments(Resource[] resources) {
    List<Document> documents = Arrays.stream(resources).map(this::getDocFromResource).toList();
    TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(300).build();
    List<Document> splitDocuments = splitter.apply(documents);
    logger.info("Number of split Documents: " + splitDocuments.size());

    return splitDocuments;
}
```

Now we have 8 documents.

### Vector Database
