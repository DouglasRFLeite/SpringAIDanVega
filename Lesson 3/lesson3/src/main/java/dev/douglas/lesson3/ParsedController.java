package dev.douglas.lesson3;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ParsedController {
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/songsArtist.st")
    private Resource songListArtistResource;

     @Value("classpath:/prompts/songsMap.st")
    private Resource songMapArtistResource;

    @Value("classpath:/prompts/songsBean.st")
    private Resource songBeanArtistResource;

    ParsedController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/songs-list/{artist}")
    public List<String> getSongListByArtist(@PathVariable(value = "artist") String artist) {
        ListOutputConverter listOutputConverter = new ListOutputConverter();

        PromptTemplate promptTemplate = new PromptTemplate(songListArtistResource);
        Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", listOutputConverter.getFormat()));

        List<String> songs = listOutputConverter.convert(chatClient.prompt(prompt).call().content());
        return songs;
    }

    @GetMapping("/songs-map/{artist}")
    public Map<String, Object> getSongMapByArtist(@PathVariable(value = "artist") String artist) {
        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        PromptTemplate promptTemplate = new PromptTemplate(songMapArtistResource);
        Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", mapOutputConverter.getFormat()));

        Map<String, Object> songs = mapOutputConverter.convert(chatClient.prompt(prompt).call().content());
        return songs;
    }

    @GetMapping("/songs-bean/{artist}")
    public ArtistBean getSongBeanByArtist(@PathVariable(value = "artist") String artist) {
        BeanOutputConverter<ArtistBean> beanOutputConverter = new BeanOutputConverter<>(ArtistBean.class);

        PromptTemplate promptTemplate = new PromptTemplate(songBeanArtistResource);
        Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", beanOutputConverter.getFormat()));

        ArtistBean artistBean = beanOutputConverter.convert(chatClient.prompt(prompt).call().content());
        return artistBean;
    }
    
}
