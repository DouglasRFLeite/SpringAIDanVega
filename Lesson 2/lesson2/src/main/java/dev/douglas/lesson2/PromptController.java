package dev.douglas.lesson2;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromptController {
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/youtube.st")
    private Resource ytPromptResource;

    PromptController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/youtube")
    public String youtube(@RequestParam(value = "genre") String genre){        
        PromptTemplate promptTemplate = new PromptTemplate(ytPromptResource);
        Prompt prompt = promptTemplate.create(Map.of("genre", genre));
        
        return chatClient.prompt(prompt).call().content();
    }

    @GetMapping("/dad")
    public String dad(){
        SystemMessage systemMessage = new SystemMessage("You're a Dad. You only tell Dad Jokes. If someone asks you to tell a joke, either make it a Dad Joke or say you can't do it.");
        UserMessage userMessage = new UserMessage("Talk to me about the 100 year war in France");
        return chatClient.prompt(new Prompt(userMessage, systemMessage)).call().content();
    }

}
