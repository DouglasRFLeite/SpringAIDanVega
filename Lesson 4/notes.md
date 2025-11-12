## Lesson 4

At this point, we're getting out of sync with Dan Vega's playlist. I don't think we need a whole lesson and notes on stuffing the prompt, but I do think we need more than one on RAG. So let's get started with something on stuffing the prompt.

### Stuffing the Prompt

Whenever I teach about AI I like to give a thorough introduction on what Context is and how it helps the model give the right answer. Unfortunetly, this is not me teaching about AI, these are my notes, so I wouldn't really take thorough notes on something I already know a lot about. So, if you don't know enough about Context, maybe take a break and find out, maybe move along and understand the pratical implications of it.

When Stuffing the Prompt, we're adding relevant information to the Prompt as **Context**. It's what you'd do if you sent a document to Chat GPT or Claude and asked questions about that document. It's included on the prompt. Awesome right?

That said, this spends more input tokens. Way more, depending on the size of what your including in your prompt. A whole book, for example, would maybe cost a couple of dolars per request. We'll see how to handle that in a minute, but I wanted to mention it to explain why we're not testing the prompt stuffing with a whole book.

Instead, we're doing it using the chapter of a book. The first chapter o the book of John, to be more specifig.

### Prompting without Stuffing

As we've done a lot before, let us start creating a simple application that prompts GPT4.

```java
@RestController
public class JohnController {

    private final ChatClient chatClient;

    JohnController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping()
    public String getJohnInfo() {
        String prompt = "Who is The Verb?";

        return chatClient.prompt(prompt).call().content();
    }

}
```

Yep, extremely simple, let's see the response:

> The Verb is a British alternative rock band that was formed in 1989. They are known for their distinctive sound, which blends elements of shoegaze, psychedelic rock, and Britpop. The band gained significant popularity in the 1990s, particularly with their albums "A Storm in Heaven" and "Urban Hymns," which featured hits like "Bitter Sweet Symphony" and "The Drugs Don't Work." The band's lineup has included members Richard Ashcroft, Nick McCabe, Simon Jones, and Pete Salisbury.  
>
> In addition to the band, "The Verb" may also refer to different contexts, such as a term in linguistics or other cultural references. If you were referring to something specific, please provide more context!  

Yep, sounds right, but isn't really the response we wanted.

### Stuffing our Prompt

We'll create two files on our `resources` folder. One for our template and one for our data.

> <{context}>
> Using the context provided above in angle brackets answer the following question:
> Who is the verb?

```txt
1 In the beginning was the Word, and the Word was with God, and the Word was God.

2 The same was in the beginning with God.

3 All things were made by him; and without him was not any thing made that was made.
...
```

Now let's use those on the code:

```java
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
```

And let us see the result:

> The Word, as described in the passage, is identified as Jesus Christ. In John 1:1, it states, "In the beginning was the Word, and the Word was with God, and the Word was God." This indicates that the Word is both divine and existed with God from the very beginning. Further in the passage, it is mentioned that "the Word was made flesh, and dwelt among us," which confirms that the Word refers to Jesus, who became incarnate.

Yep, sounds more like it.

This is how we stuff our prompt.

### RAG Motivation

So, before I start on what's RAG, I really like thinking why RAG.

In our example, we stuffed our prompt with the first chapter of the book of John. You can imagine our prompt got overall a lot bigger than just the question. That said, the first chapter of the book of John represents roughly 0.18% of the whole Bible.

Yep, I want you to think about it for a second.

That means that, if I want to allow this system of mine to answer any Bible-related question, I would have to stuff my prompt with something about 500 times bigger than I did in this example. I'll run out of tokens in a single prompt.

That's where the RAG comes in.

It's **a way to find relevant documents for the question**. That way, when we ask a question about John, the RAG should bring us the book of John and stuff our prompt with it.

### Vectors and Embeddings

But how do we know a document is relevant to that question? Think of a Google Search. But better. ...maybe.

So, our first intuition is to do a syntatic search. That is, look for similar words. If I search the whole Bible for "The Word" it will probably point to the first chapter of John. In other words, it works, we can use syntatic searches for that.

That said, I wouldn't be able to find anything searching for "The Verb", if for example I forgot which term is actually used by John. It may not be the best example ever, but you get the point.

We need **semantic searching**, search for similar meaning. And that's where Vectors and Embeddings come.

The idea here (and behind LLMs in general, if you dig a bit deeper) is to turn words, phrases, question and documents into numbers, into a big array (or else, Vector) of numbers. That's what we call **Embedding**. That way, we can query for the **distance between two vectors**.

Thinking about vectors like this is a very Linear Algebra way of thinking, so we won't go too deep into it.

Spring AI can't provide us with Vector Databases or Embedding Models, but it provides us with Clients for both that will make our access much easier.

### RAGs

So, at the end of the day, what happens in a RAG is:

1. We take all of our documents (or chunks, pieces of them) and we **Embedd** them. That is, we turn them into an array of numbers that represent their meaning:

```txt
"2 The same was in the beginning with God."
 |
 V
[0.87, 0.12, 0.56, ...]
```

2. We store those vectors along with references to the content in a **Vector Database**, we'll see more about them in the future.

3. We take the user's question and we **Embedd** it, generating a new vector:

```txt
"Who is the verb?"
 |
 V
[0.78, 0.21, 0.65, ...]
```

4. We query our **Vector Database** for vectors close to the one of the question.

5. We stuff our prompt with the content referenced by those vectors.

6. We give a fundamented response to the user.

And that's it. That's roughly the intuition and the theory behind RAGs. Not much practice but a lot of theory. We'll get more practice in the next lesson.
