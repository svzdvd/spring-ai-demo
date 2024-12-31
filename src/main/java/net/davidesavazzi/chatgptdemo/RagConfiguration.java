package net.davidesavazzi.chatgptdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class RagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagConfiguration.class);

    @Value("classpath:/docs/presidential_2024.txt")
    private Resource ragTextData;

    @Value("vector_store.json")
    private String vectorStoreName;

    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        var vectorStore = new SimpleVectorStore(embeddingModel);
        var vectorStoreFile = getVectorStoreFile();
        if (vectorStoreFile.exists()) {
            log.info("Vector Store file already exist");
            vectorStore.load(vectorStoreFile);
        } else {
            log.info("Creating Vector Store file...");
            var textReader = new TextReader(ragTextData);
            textReader.getCustomMetadata().put("filename", "presidential_2024.txt");
            var documents = textReader.get();
            var tokenTextSplitter = new TokenTextSplitter();
            var splitDocuments = tokenTextSplitter.split(documents);

            vectorStore.add(splitDocuments);
            vectorStore.save(vectorStoreFile);
        }

        return vectorStore;
    }

    private File getVectorStoreFile() {
        var path = Paths.get("src", "main", "resources", "data");
        return new File(path.toFile().getAbsolutePath() + "/" + vectorStoreName);
    }
}
