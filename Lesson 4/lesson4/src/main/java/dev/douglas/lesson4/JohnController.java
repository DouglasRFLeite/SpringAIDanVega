package dev.douglas.lesson4;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JohnController {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/john.st")
    private Resource prompt;

    @Value("classpath:data/john1.txt")
    private Resource data;

    JohnController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping()
    public String getJohnInfo() {
        PromptTemplate promptTemplate = new PromptTemplate(prompt);
        Prompt prompt = promptTemplate.create(Map.of("context", data));

        return chatClient.prompt(prompt).call().content();
    }

}
