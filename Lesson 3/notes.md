## Lesson 3

We noticed at this point that we don't really need SpringAI to work with LLMs via API, we could, indeed, just make normal HTTP requests. That said, what SpringAI provides is a set of tools, functions and Wrappers that make more complex interactions with the models not only possible but easy. Today we'll learn about one of those tools called Output Parser.

### Motivation

So far we have not developed anything more than what we can do in ChatGPT, but we will, that's why we're learning how to interact with AI from a Java Application. That said, we've only been receiving Strings.

OutputParser is here to help us properly parse our output and use it somewhere else.

There are 3 more important kinds of OutputParser we'll see today: List, Map and Bean Parsers.

### Creating the Application

We can go ahead and create our application same way we did before (I won't go over it again, fallback to previous lessons).

We'll also set up a ParsedController with a sample endpoint. The endpoint will use a PromptTemplate with a StringTemplate stored on the resources file. Look up the previous lessons if any of that sound strange to you.

> Please give me a list of top 10 songs for the artist {artist}. If you don't know, just say it.

```java
@RestController
public class ParsedController {
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/songsArtist.st")
    private Resource songsArtistResource;

    ParsedController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/songs/{artist}")
    public String getSongsByArtist(@PathVariable(value = "artist") String artist) {
        PromptTemplate promptTemplate = new PromptTemplate(songsArtistResource);
        Prompt prompt = promptTemplate.create(Map.of("artist", artist));

        return chatClient.prompt(prompt).call().content();
    }
    
}
```

Just a note here: *I'm extremely dissatisfied with this amount of unecessary verbosity. I'm not saying it's a bad move on SpringAI, verbosity on Java gives us power and flexibility without giving up on clarity and safety. That said, for these simple examples, I wish I had a superclass or interface to wrap all of this up for me. Maybe I'll think of something like that in the future.*

Well, we can run this code and curl it with `curl localhost:8080/songs/Hillsong` to get:

> Here are ten popular songs by Hillsong, known for their impactful worship music:  
>
> 1. **What a Beautiful Name**  
> 2. **Oceans (Where Feet May Fail)**  
> 3. **Who You Say I Am**  
> 4. **So Will I (100 Billion X)**  
> 5. **Cornerstone**  
> 6. **Mighty to Save**  
> 7. **Grace to Grace**  
> 8. **Evermore**  
> 9. **Hosanna**  
> 10. **King of Kings**  
>
> These songs have been widely used in worship settings and have resonated with many listeners around the world

Yep, same old same old. Let's take a step forward now. 

### List Parser

That's a String format, it works to answer the user but nothing asures me GPT won't put 2 paragraphs at the top, use * instead of 1., etc. So will now attempt to get a List of Strings from that.

For that, we'll use an OutputParser:

**It's not Parser, its Converter!**

If you didn't know, I have been following a playlist of Spring AI videos by Dan Vega and taking notes (not sure if that will be included in the header in the future). Thus, this is not intended to be a formal, well structured public-focused article. It's my notes. So, since I only now figured out they we're called Parsers when Dan recorded but now are called Converters, I will not correct any previous mentioning of the work Parsers. Let us use Converters from now on.

### List Converter

This is the ListOutputConverter:

```java
public class ListOutputConverter extends AbstractConversionServiceOutputConverter<List<String>> {

	public ListOutputConverter() {
		this(new DefaultConversionService());
	}

	public ListOutputConverter(DefaultConversionService defaultConversionService) {
		super(defaultConversionService);
	}

	@Override
	public String getFormat() {
		return """
				Respond with only a list of comma-separated values, without any leading or trailing text.
				Example format: foo, bar, baz
				""";
	}

	@Override
	public List<String> convert(@NonNull String text) {
		return this.getConversionService().convert(text, List.class);
	}

}
```

You can already see it doesn't do anything too fancy, it just appends that format to the end of our prompt using PromptTemplates. That means 3 things:

1. **We can do this ourselves** - as mentioned at the beginning, Spring AI is not here to turn LLM comunication possible, it's here only to make it easier and give us some stuff out of the box.
2. **We can create one ourselves** - if we want something other than Lists, Beans or Maps, we can create it.
3. **It costs us tokens** - I've only mentioned good things about it this far, but this is a bad one. We should expect it, but know we know for sure that this costs us a couple more tokens per LLM API call and we must be aware of that.

I was going to show you a deeper dive into the `AbstractConversionServiceOutputConverter` and what it extended, but it's mostly OOP shenanigans, no relevant functionality to look into. That said, I do recommend you take a peek into the `DefaultConversionService`. Those guys actually do some hard work an I appreciate having them ready for me.

Let's get back to the code.

```java
@GetMapping("/songs/{artist}")
public List<String> getSongsByArtist(@PathVariable(value = "artist") String artist) {
    ListOutputConverter listOutputConverter = new ListOutputConverter();

    PromptTemplate promptTemplate = new PromptTemplate(songsArtistResource);
    Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", listOutputConverter.getFormat()));

    List<String> songs = listOutputConverter.convert(chatClient.prompt(prompt).call().content());
    return songs;
}
```

We made a couple of changes to our code:

* We added the ListOutputConverter at the top and created it - *another thing that could be made easier and less verbose*;
* We are passing the format to the PromptTemplate;
* We are using the same converter to convert the response String into a list;
* We change the return type of the endpoint to `List<String>`;

That's not all tho. We still need to use thar format in our template. So we're changing it to look like this:

> Please give me a list of top 10 songs for the artist {artist}.  
> If you don't know, just say it.  
> {format}

Now we can finally try it out with the same command:

>["What a Beautiful Name","Oceans (Where Feet May Fail)","Who You Say I Am","What a Friend","So Will I (100 Billion X)","Mighty to Save","Hosanna","Cornerstone","Broken Vessels (Amazing Grace)","Grace to Grace"]

### Map Converter

On top of the List Converter, we also have a Map Converter (and a BeanConverter we'll see next).

This is the MapConverter class:

```java
public class MapOutputConverter extends AbstractMessageOutputConverter<Map<String, Object>> {

	public MapOutputConverter() {
		super(new MappingJackson2MessageConverter());
	}

	@Override
	public Map<String, Object> convert(@NonNull String text) {
		if (text.startsWith("```json") && text.endsWith("```")) {
			text = text.substring(7, text.length() - 3);
		}

		Message<?> message = MessageBuilder.withPayload(text.getBytes(StandardCharsets.UTF_8)).build();
		return (Map) this.getMessageConverter().fromMessage(message, HashMap.class);
	}

	@Override
	public String getFormat() {
		String raw = """
				Your response should be in JSON format.
				The data structure for the JSON should match this Java class: %s
				Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
				Remove the ```json markdown surrounding the output including the trailing "```".
				""";
		return String.format(raw, HashMap.class.getName());
	}

}
```

We can already notice it's a bit more complicated and thoughtful than the List one. Still doable by hand, but far more difficult. Let us test it out. 

I had to make some changes to the overall class to make this working and clear:

```java
@RestController
public class ParsedController {
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/songsArtist.st")
    private Resource songListArtistResource;

     @Value("classpath:/prompts/songsMap.st")
    private Resource songMapArtistResource;

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
    
}
```

Focusing on the second one we can see it's pretty similar, just changing the name of some stuff. Now, I did have to change the prompt a bit too, so it could ask for more than just name:

> Please give me a map of top 10 songs for the artist {artist} and the amount of awards, as far as you know, for each song.  
> If you don't know, just say it.  
> {format}


And this was the result:

> {"Hillsong":{"What a Beautiful Name":{"awards":"1"},"Oceans (Where Feet May Fail)":{"awards":"2"},"Who You Say I Am":{"awards":"1"},"What a Savior":{"awards":"0"},"Mighty to Save":{"awards":"2"},"Cornerstone":{"awards":"0"},"So Will I (100 Billion X)":{"awards":"1"},"King of Kings":{"awards":"0"},"Another in the Fire":{"awards":"0"},"This I Believe (The Creed)":{"awards":"0"}}}

I should remind you I don't care if it's true or not, that's on OpenAI. Right now I just care about formating. 

### Bean Converter

Let us take a look at it:

```java
public BeanOutputConverter(Class<T> clazz) {
    this(ParameterizedTypeReference.forType(clazz));
}
```

Not, this is obviously not the whole class. The whole class is too big for me to put in here. But this shows the interesting fact that this needs a base class to work. Let us create that:

```java
public record MusicBean(String musicName, Integer numAwards) {}

public record ArtistBean(String artistName, String origin, String style, List<MusicBean> topSongs) {}
```

Now we can properly use the `BeanOutputParser` in our code:

```java
@GetMapping("/songs-bean/{artist}")
public ArtistBean getSongBeanByArtist(@PathVariable(value = "artist") String artist) {
    BeanOutputConverter<ArtistBean> beanOutputConverter = new BeanOutputConverter<>(ArtistBean.class);

    PromptTemplate promptTemplate = new PromptTemplate(songBeanArtistResource);
    Prompt prompt = promptTemplate.create(Map.of("artist", artist, "format", beanOutputConverter.getFormat()));

    ArtistBean artistBean = beanOutputConverter.convert(chatClient.prompt(prompt).call().content());
    return artistBean;
}
```

We should also use a new prompt:

> Please find me some information about the artist {artist}.  
> Also give me a list of the top 10 songs for that artist and the amount of awards, as far as you know, for each song.  
> If you don't know, just say it.  
> {format}  

And Voi lá:

> {"artistName":"Hillsong","origin":"Australia","style":"Contemporary Christian Music, Worship","topSongs":[{"musicName":"What a Beautiful Name","numAwards":0},{"musicName":"Oceans (Where Feet May Fail)","numAwards":0},{"musicName":"Mighty to Save","numAwards":0},{"musicName":"Who You Say I Am","numAwards":0},{"musicName":"This I Believe (The Creed)","numAwards":0},{"musicName":"So Will I (100 Billion X)","numAwards":0},{"musicName":"What a Friend","numAwards":0},{"musicName":"God So Loved","numAwards":0},{"musicName":"The Stand","numAwards":0},{"musicName":"Here I Am to Worship","numAwards":0}]}

This is how you should use OutputConverters in Spring AI.
