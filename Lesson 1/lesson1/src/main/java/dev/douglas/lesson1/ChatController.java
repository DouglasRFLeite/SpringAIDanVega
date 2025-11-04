package dev.douglas.lesson1;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ChatController {
    
    private final ChatClient chatClient;

    ChatController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping()
    public String getDadJoke() {
        return chatClient.prompt("Tell me a dad joke").call().content();
    }
    
}
