package dev.douglas.lesson3;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ParsedController {
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/songsArtist.st")
    private Resource songsArtistResource;

    ParsedController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/songs/{artist}")
    public List<String> getSongsByArtist(@PathVariable(value = "artist") String artist) {
        ListOutputConverter listOutputConverter = new ListOutputConverter();

        PromptTemplate promptTemplate = new PromptTemplate(songsArtistResource);
        Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", listOutputConverter.getFormat()));

        List<String> songs = listOutputConverter.convert(chatClient.prompt(prompt).call().content());
        return songs;
    }
    
}
