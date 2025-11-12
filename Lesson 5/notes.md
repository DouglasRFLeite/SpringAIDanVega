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

Now we need to **Embedd** our text and store our **Vectors** somewhere. Spring AI uses an interface called `VectorStore` to interact with multiple diferent Vector Databases. 

```java
public interface VectorStore extends DocumentWriter {

    default String getName() {
		return this.getClass().getSimpleName();
	}

    void add(List<Document> documents);

    void delete(List<String> idList);

    void delete(Filter.Expression filterExpression);

    default void delete(String filterExpression) { ... };

    List<Document> similaritySearch(String query);

    List<Document> similaritySearch(SearchRequest request);

    default <T> Optional<T> getNativeClient() {
		return Optional.empty();
	}
}
```

We also have a few possible implementations of the interface, one for each vendor or different Vector Database provider. For educational purposes, they provide one named `SimpleVectorStore` that stores everything in a json file. We'll use that one.

Spring AI also attaches the Embedding functionality inside the VectorStores by injecting the Embedding Client inside the VectorStore. Let us create ours:

```java
@Bean
SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) throws IOException {
    SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
    List<Document> documents = getDocuments();
    simpleVectorStore.add(documents);
    return simpleVectorStore;
}
```

Sounds simple, but I can't really say it is simple. Spring AI is actually doing a lot of it for us.

By injecting the EmbeddingModel we are reaching out to our OpenAI dependency and fething their EmbeddingModel Implementation. When we add the documents to the Vector Store, it makes an API call to OpenAI's EmbeddingModel for each document, we even get logs for it:

> o.s.ai.vectorstore.SimpleVectorStore     : Calling EmbeddingModel for document id = 110fa912-2732-4cab-ad57-04e867725c30

Then it generates, inside the VectorStore, a collection of SimpleVectorStoreContent, which stores the embedding, the document's content and some metadata. That will be our Vector Database for now.

Our code is enough for us to access the Vector Database from memory. But, as we saw, it makes a lot of API calls to OpenAI, and that means money. Thus, we will store those Embeddings in a file so we can retrieve from it later.

```java
@Value("${rag.vectorstore}")
private String vectorStoreFilename = "vectorstore.json";

@Bean
SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) throws IOException {

    SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
    File vectorStorFile = Paths.get(vectorStoreFilename).toFile();
    if(vectorStorFile.exists()){
        logger.info("Vector Store file exists");
        simpleVectorStore.load(vectorStorFile);
    } else {
        logger.info("Vector Store file does not exist, embedding documents");
        List<Document> documents = getDocuments();
        simpleVectorStore.add(documents);
        simpleVectorStore.save(vectorStorFile);
    }

    return simpleVectorStore;
}
```

We can take a look at how the json looks:

```json
{
  "8fe425df-9d4e-4afb-8aa8-34bf710d83db" : {
    "text" : "47 Jesus saw Nathanael coming to him, and saith of him, Behold an Israelite indeed, in whom is no guile!\n\n48 Nath"...,
    "embedding" : [ 0.002511601, -0.009491174, -0.022052445, -0.0114312135, -0.03023066, 0.01339085, -0.0061434605, -0.0021996922, -0.03469863, ...]
    "id" : "8fe425df-9d4e-4afb-8aa8-34bf710d83db",
    "metadata" : {
      "filename" : "john1.txt",
      "source" : "john1.txt",
      "charset" : "UTF-8"
    }
  },
```

And we are good to go on the Vector Database side of things!

### Querying

Now we're moving back to the model comunication side of our RAG, let's create the Controller.

```java
@RestController
public class JohnController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    JohnController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("search")
    public List<Document> semanticSearch(@RequestParam(value = "message", defaultValue = "Who is The Word?") String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(2)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

}
```

This is enough for us to see the power of the **Vector Database** and the Semantic Search. By hitting that endpoint we can already see results... we're just not writing it here.

### Stuffing the Prompt

That's all that's left, let us stuff our prompt with the data. We'll use this template:

> You are a helpful assistant, conversing with an user about the subjects contained in a set of documents.  
> Use the information from the DOCUMENTS section to provide accurate answers.
> If unsure or if the answer isn't found in the DOCUMENTS section, say you don't know.
>
> QUESTION:
> {input}
>
> DOCUMENTS:
> {documents}

And create a new Get method:

```java
@GetMapping("ask")
public String promptRAG(@RequestParam(value = "message", defaultValue = "Who is The Word?") String query) {
    List<Document> documents = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(2)
            .build()
    );
    String documentsString = documents.stream().map(Document::getText).reduce("", (s, d) -> s + "\n" + d);

    PromptTemplate promptTemplate = new PromptTemplate(johnPrompt);
    Prompt prompt = promptTemplate.create(Map.of("input", query, "documents", documentsString));
    
    return chatClient.prompt(prompt).call().content();
}
```

And it magicaly works:

> The Word refers to Jesus Christ. According to the documents, "In the beginning was the Word, and the Word was with God, and the Word was God." The Word is described as being involved in creation, having life, and being the light of men. Furthermore, the documents state that "the Word was made flesh, and dwelt among us," indicating the incarnation of Jesus.

That's about it, we can already create a RAG!
