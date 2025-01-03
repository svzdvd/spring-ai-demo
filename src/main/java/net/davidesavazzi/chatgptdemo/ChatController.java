package net.davidesavazzi.chatgptdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient.Builder builder;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/youtube.st")
    private Resource ytPromptResource;

    @Value("classpath:/prompts/stuff-it.st")
    private Resource stuffItPromptResource;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragPromptResource;

    @Autowired
    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.builder = builder;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return builder.build().prompt(message).call().content();
    }

    @GetMapping("/joke")
    public String chatWithPrompt() {
        var prompt = new Prompt(new UserMessage("Tell me a dad joke"));
        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();
        log.info("response: {}", response.getMetadata());
        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/youtube")
    public String chatWithPromptTemplate(@RequestParam(value = "genre", defaultValue = "tech") String genre) {
        var promptTemplate = new PromptTemplate(ytPromptResource);
        var prompt = promptTemplate.create(Map.of("genre", genre));

        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();
        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/dad-joke")
    public String chatWithSystemMessage() {
        var system = new SystemMessage("Your primary function is to tell Dad jokes. " +
                "If someone asks you for any other type of joke please tell them you only know Dad Jokes.");
        var user = new UserMessage("Tell me a Dad Joke");
        var prompt = new Prompt(List.of(system, user));
        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();
        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/youtube-list")
    public String chatWithListOutputConverter(@RequestParam(value = "genre", defaultValue = "tech") String genre) {
        var promptTemplate = new PromptTemplate("List 10 of the most popular YouTube channels in the {genre} genre");
        var message = promptTemplate.createMessage(Map.of("genre", genre));

        ListOutputConverter converter = new ListOutputConverter(new DefaultConversionService());
        var formatMessage = new SystemMessage(converter.getFormat());

        var prompt = new Prompt(List.of(message, formatMessage));
        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();
        var items = converter.convert(response.getResult().getOutput().getContent());
        return Objects.requireNonNull(items).toString();
    }

    @GetMapping("/youtube-map")
    public String chatWithMapOutputConverter(@RequestParam(value = "genre", defaultValue = "tech") String genre ) {
        var promptTemplate = new PromptTemplate(ytPromptResource);
        var message = promptTemplate.createMessage(Map.of("genre", genre));

        MapOutputConverter converter = new MapOutputConverter();
        var formatMessage = new SystemMessage(converter.getFormat());

        var prompt = new Prompt(List.of(message, formatMessage));
        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();

        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/books-by-author")
    public Author chatWitBeanOutputConverter(@RequestParam(value = "author", defaultValue = "Philip Dick") String author) {
        var promptTemplate = new PromptTemplate("List the books written by the author {author}. " +
                "If you aren't positive that a book belongs to this author please don't include it.");
        var message = promptTemplate.createMessage(Map.of("author", author));

        var converter = new BeanOutputConverter<>(Author.class);
        var formatMessage = new SystemMessage(converter.getFormat());

        var prompt = new Prompt(List.of(message, formatMessage));
        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();

        return converter.convert(response.getResult().getOutput().getContent());
    }

    @GetMapping("/stuff-test")
    public String chatWithPromptStuffing(@RequestParam(value = "stuffit", defaultValue = "false") boolean stuffit) {
        var promptTemplate = new PromptTemplate(stuffItPromptResource);

        var params = new HashMap<String,Object>();
        params.put("question", "What max value BTC reached in October 2024?");
        if (stuffit) {
            params.put("context", "BTC in October 2024 reached an all time high of 98k $");
        } else {
            params.put("context", "");
        }

        var message = promptTemplate.createMessage(params);
        var response = builder.build()
                .prompt(new Prompt(message))
                .call().chatResponse();
        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/presidential")
    public String chatAbout2024PresidentialElections(@RequestParam(value = "question", defaultValue = "Who won the 2024 presidential elections?") String question) {
        var similarDocuments = vectorStore.similaritySearch(question);
        var contentList = similarDocuments.stream().map(Document::getContent).toList();

        var promptTemplate = new PromptTemplate(ragPromptResource);

        var promptParameters = new HashMap<String,Object>();
        promptParameters.put("input", question);
        promptParameters.put("documents", String.join("\n", contentList));

        var prompt = promptTemplate.create(promptParameters);

        var response = builder.build()
                .prompt(prompt)
                .call().chatResponse();
        return response.getResult().getOutput().getContent();
    }
}
