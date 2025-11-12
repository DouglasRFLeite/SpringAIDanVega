package dev.douglas.lesson5;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JohnController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/john.st")
    private Resource johnPrompt;

    JohnController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("search")
    public List<Document> semanticSearch(
            @RequestParam(value = "message", defaultValue = "Who is The Word?") String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(2)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

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
    

}
