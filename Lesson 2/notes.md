## Lesson 2

This is supposed to be a quick lesson about how to properly handle more complex prompts with Spring AI.

Here is where it starts to make more sense to use Spring AI (or any other framework for that matter) instead of just accessing the LLM via API as you would with any other API. Spring AI gives us a structured way to generate more complex prompts with roles, placeholders, templates, etc.

Prompt Engineering is an awkward buzzword but sums up well the need to properly prepare our prompts in order for the LLM to give us the correct (as per our expectation) answer.

### The Prompt Class

Spring AI gives us a Prompt class:

```java
public class Prompt implements ModelRequest<List<Message>> {
   private final List<Message> messages;
   @Nullable
   private ChatOptions chatOptions;

   public Prompt(String contents) {
      this((Message)(new UserMessage(contents)));
   }

   public Prompt(Message message) {
      this(Collections.singletonList(message));
   }

   public Prompt(List<Message> messages) {
      this((List)messages, (ChatOptions)null);
   }

   public Prompt(Message... messages) {
      this((List)Arrays.asList(messages), (ChatOptions)null);
   }

   public Prompt(String contents, @Nullable ChatOptions chatOptions) {
      this((Message)(new UserMessage(contents)), chatOptions);
   }

   public Prompt(Message message, @Nullable ChatOptions chatOptions) {
      this(Collections.singletonList(message), chatOptions);
   }

   public Prompt(List<Message> messages, @Nullable ChatOptions chatOptions) {
      Assert.notNull(messages, "messages cannot be null");
      Assert.noNullElements(messages, "messages cannot contain null elements");
      this.messages = messages;
      this.chatOptions = chatOptions;
   }
```

Just looking at this amount of constructroes we see interesting stuff like `UserMessage` and `List<Messages>` that can give us a heads-up idea of how much it can do to help us. Besides `UserMessage` there are a few more classes that extend `Message`, they define roles for each message:

* **System Role** - Guides the AI's behaviour and response style;
* **User Role** - Represents the user's input, questions or commands. It's the basis for the AI response;
* **Assistant Role** - The AI's response to the user input, important to track the previous responses and maintain a conversational flow.
* **Function Role** - This role deals with specific tasks or operations during the conversation.

We can and should use all of these roles to enhance the power and consistency of the model.

### Creating an Application

Having already completed the steps from the first lesson, I can jump to creating the application. I'll again fallback to Spring Initialzr with these specs:

1. Maven
2. Spring 3.5.7
3. Jar packaging
4. Java 21
5. Dependencies:
    * Spring Web
    * OpenAI
    * Lombok
    * Spring Boot DevTools

And add this to my properties file:

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.model=gpt-4
```

At this point I can run the application but, as expected, it won't do anything.

### Prompt Controller

Let us create a controller so our application does something.

```java
@RestController
public class PromptController {
    
    private final ChatClient chatClient;

    PromptController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/dad")
    public String dad(){
        return chatClient.prompt(new Prompt("Tell me a dad joke")).call().content();
    }
}
```

Instead of just putting in a String we now used a Prompt object. For this example it made no diference, but it will soon. This is our response for `localhost:8080/dad`:

>Why did the scarecrow win an award? Because he was outstanding in his field!

### Prompt Templates

The first Prompt benefit I'll explore is the Prompt Template, that enables me to add a sort of variable inside my prompt.

```java
@GetMapping("/youtube")
public String youtube(@RequestParam(value = "genre") String genre){
   String message = """
            List 10 of the most popular Youtubers in {genre} along with their current subscriber counts.
            If you don't know the answer, just say you don't know. Don't make anything up. 
            """;
   
   PromptTemplate promptTemplate = new PromptTemplate(message);
   Prompt prompt = promptTemplate.create(Map.of("genre", genre));
   
   return chatClient.prompt(prompt).call().content();
}
```

Here we can see the first String has a `{genre}` placeholder. When we create the Prompt using the PromptTemplate we provide a Map with the values of each placeholder.

If we call `http://localhost:8080/youtube?genre=tech` we get:

> I don't have real-time data access to provide current subscriber counts for YouTubers. However, I can list some of the most popular tech YouTubers known for their content as of my last update: 1. Marques Brownlee (MKBHD) 2. Linus Tech Tips 3. Unbox Therapy 4. Dave Lee (Dave2D) 5. Justine Ezarik (iJustine) 6. Michael Fisher (MrMobile) 7. Austin Evans 8. Sara Dietschy 9. The Verge 10. TechCrunch For the latest subscriber counts, I recommend checking their YouTube channels directly.

If we call `http://localhost:8080/youtube?genre=fitness`:

>I don't have access to real-time data, including current subscriber counts for YouTubers. However, I can provide a list of some popular fitness YouTubers as of my last update: 1. **Chloe Ting** 2. **Blogilates (Cassey Ho)** 3. **Jeff Nippard** 4. **Whitney Simmons** 5. **Natacha Océane** 6. **Chris Heria** 7. **Pamela Reif** 8. **Athlean-X (Jeff Cavaliere)** 9. **Fitness Blender** 10. **Sarah's Day** For the most current subscriber counts, I recommend checking their YouTube channels directly.

### System Role Prompt

Now we're taking a look into the Roles by separating the SystemMessage and UserMessage on the dad jokes endpoint.

```java
@GetMapping("/dad")
public String dad(){
   UserMessage userMessage = new UserMessage("Tell me a joke about cats");
   return chatClient.prompt(new Prompt(userMessage)).call().content();
}
```

This first change doesn't really do much. It seems the default behaviour of the Prompt class when receiving a String is to turn it into a UserMessage. This is our result:

>Why did the cat sit on the computer? Because it wanted to keep an eye on the mouse!

But know we can create a separate message called SystemMessage that will give a more broad instruction to the model.

```java
@GetMapping("/dad")
public String dad(){
   SystemMessage systemMessage = new SystemMessage("You're a Dad. You only tell Dad Jokes. If someone asks you to tell a joke, either make it a Dad Joke or say you can't do it.");
   UserMessage userMessage = new UserMessage("Tell me a joke about dogs");
   return chatClient.prompt(new Prompt(userMessage, systemMessage)).call().content();
}
```

Now we can make sure our model will only tell dad jokes like this one:

> Why did the dog sit in the shade? Because he didn't want to become a hot dog!

But if we ask it to do something too diferent it shouldn't.

```java
UserMessage userMessage = new UserMessage("Talk to me about the 100 year war in France");
```

Or it can do this...

> The Hundred Years' War was a long conflict between England and France that lasted from 1337 to 1453. It was marked by a series of battles over territorial disputes and claims to the French throne. Now, speaking of long wars, why did the dad bring a ladder to the bar? Because he heard the drinks were on the house!

As you can see LLMs are not entirely relyable.

### Template Resources

To wrap up this lesson we're going to look into a very good practice on storing this Prompt Templates.

We can create a prompts folder inside resources: `src/main/resources/prompts` and save our prompts there with the `.st` extension. "st" stands for String Template I believe.

We can then referece that template from our class instead of writing the whole string there:

```java
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
```

And, as we can see, it works for `http://localhost:8080/youtube?genre=games`:

>I don't have access to real-time data, so I cannot provide current subscriber counts for YouTubers. However, as of my last update, here are 10 popular gaming YouTubers (in no particular order): 1. PewDiePie 2. Markiplier 3. Jacksepticeye 4. Ninja 5. VanossGaming 6. DanTDM 7. KSI 8. Pokimane 9. Dream 10. SSundee For up-to-date subscriber counts, I recommend checking their YouTube channels directly or using a site that tracks YouTube statistics.

This ends our second lesson, a bit about Prompts. Not a lot of Prompt Engineering but that you can find elsewhere (or maybe we will talk about it in the near future).
