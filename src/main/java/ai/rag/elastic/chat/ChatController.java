
package ai.rag.elastic.chat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("api/ChatController")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;

    private ChatModel chatModel;
    private final ElasticsearchClient elasticsearchClient;

    private final String PROMPT_BLUEPRINT = """
      You are a helpful assistant tasked with generating 5 unique business names similar to the one provided in the QUESTION section.
      Use the information from the DOCUMENTS section, which contains a database of matching business names, to suggest similar names.
      Do not set any explanations or descriptions.
      The generated business names should be:
      Similar to the one in the QUESTION.
      Unique (not an exact match with any name in the DOCUMENTS).
      If you cannot find suitable matches or are unsure, simply state that you don’t know the answer.
    
      QUESTION:
      {query}
    
      DOCUMENTS:
      {context}
    """;

    private final String PROMPT_BLUEPRINT_AR = """
      You are a helpful assistant tasked with generating 5 unique business names similar to the one provided in the QUESTION section.
      Use the information from the DOCUMENTS section, which contains a database of matching business names, to suggest similar names.
      The generated business names should be:
      Similar to the one in the QUESTION.
      Unique (not an exact match with any name in the DOCUMENTS).
      In the Arabic language.
      If you cannot find suitable matches or are unsure, simply state that you don’t know the answer.
    
      QUESTION:
      {query}
    
      DOCUMENTS:
      {context}
    """;



    public ChatController(ChatClient.Builder builder
    , ChatModel chatModel, ElasticsearchClient elasticsearchClient) {
        this.chatModel = chatModel;
        this.chatClient = builder.build();
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping("chat")
    public String home(@RequestBody MyPrompt myPrompt)  {


//        return chatClient.prompt().messages()
//                .user(myPrompt.getMyRequest())
//                .call()
//                .content();
        return  chat(myPrompt.getMyRequest());
    }

    @GetMapping("chatArabic")
    public String homeArabic(@RequestBody MyPrompt myPrompt)  {


//        return chatClient.prompt().messages()
//                .user(myPrompt.getMyRequest())
//                .call()
//                .content();
        return  chatArabic(myPrompt.getMyRequest());
    }

    public String chat(String query) {
        Message  sys = createPromptSys(query, searchData(query));
        PromptTemplate userTemp = new PromptTemplate("{query}");
        Message user = userTemp.createMessage(Map.of("query", query));

        var p = new Prompt(List.of(sys,user));
        ChatResponse res = chatClient.prompt(p).call().chatResponse();
        List<Generation> results = res.getResults();

        log.info("res meta " + res.getMetadata());
        log.info("res res " + res.getResults());
        return res.getResult().getOutput().getContent();
        //chatClient.prompt(p).call().getContent();
        //return chatModel.call(createPrompt(query, searchData(query)));
    }

    public String chatArabic(String query) {
        return chatModel.call(createPromptArabic(query, searchDataArabic(query)));
    }

    private List<Document> searchData(String query) {
        try {
            return testElastic(query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private List<Document> searchDataArabic(String query) {
        try {
            return testElasticArabic(query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createPrompt(String query, List<Document> context) {
        PromptTemplate promptTemplate = new PromptTemplate(PROMPT_BLUEPRINT);
        promptTemplate.add("query", query);
        promptTemplate.add("context", context);
        return promptTemplate.render();
    }

    private Message createPromptSys(String query, List<Document> context) {
        SystemPromptTemplate  promptTemplate = new SystemPromptTemplate(PROMPT_BLUEPRINT);
        promptTemplate.add("query", query);
        promptTemplate.add("context", context);
        return promptTemplate.createMessage();
    }

    private String createPromptArabic(String query, List<Document> context) {
        PromptTemplate promptTemplate = new PromptTemplate(PROMPT_BLUEPRINT_AR);
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + query);
        promptTemplate.add("query", query);
        promptTemplate.add("context", context);
        return promptTemplate.render();
    }


    public List<Document> testElastic(String queryElastic) throws IOException {

        //ElasticsearchClient client = getElasticsearchClient();
        SearchRequest searchRequest = getSearchRequest(queryElastic,
                "trade_name_en", "70%");
        SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        log.info("result " + searchResponse.hits().toString());
        List<Document> trds = new ArrayList<Document>();
        for (Hit<Object> hit : searchResponse.hits().hits()) {

            String sourceJson = mapper.writeValueAsString(hit.source());
            ObjectMapper mapper2 = new ObjectMapper();
            JsonNode actualObj2 = mapper2.readTree(sourceJson);
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("business_name",  "The company business name in english");
            trds.add(new Document( actualObj2.get("trade_name_en").asText(), metaData));
            log.info("trd is" + actualObj2.get("trade_name_en").asText());

        }
        log.info("elastic finished");
        return trds;
    }

    public List<Document> testElasticArabic(String queryElastic) throws IOException {

        //ElasticsearchClient client = getElasticsearchClient();
        SearchRequest searchRequest = getSearchRequest(queryElastic,
                "trade_name_ar", "70%");
        SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        log.info("result " + searchResponse.hits().toString());
        List<Document> trds = new ArrayList<Document>();
        for (Hit<Object> hit : searchResponse.hits().hits()) {

            String sourceJson = mapper.writeValueAsString(hit.source());
            ObjectMapper mapper2 = new ObjectMapper();
            JsonNode actualObj2 = mapper2.readTree(sourceJson);
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("business_name",  "The company business name in arabic");
            trds.add(new Document( actualObj2.get("trade_name_ar").asText(), metaData));
            log.info("trd is" + actualObj2.get("trade_name_ar").asText());

        }
        log.info("elastic finished");
        return trds;
    }

    public record TradeNamesResponse(String actor, List<String> movies) {

    }

    public static SearchRequest getSearchRequest(String queryText, String fieldName, String percentage) {
        Query matchPhraseQuery = MatchPhraseQuery.of(mp -> mp
                .field(fieldName)
                .query(queryText)
                .boost(8.0f)
                .queryName("phrase on name")
        )._toQuery();

        // Match query with AND operator and boost
        Query matchQueryAllTerms = MatchQuery.of(mq -> mq
                .field(fieldName)
                .query(queryText)
                .operator(Operator.And)
                .boost(4.0f)
                .queryName ("all terms on name")
        )._toQuery();

        // Match query with OR operator and boost
        Query matchQueryOneTerm = MatchQuery.of(mq -> mq
                .field(fieldName)
                .query(queryText)
                .operator(Operator.Or)
                .boost(1.8f)
                .queryName("at least one term on name")
        )._toQuery();

        // Match query with OR operator and boost
        Query matchQuery75Term = MatchQuery.of(mq -> mq
                .field(fieldName)
                .query(queryText)
                .boost(4.8f)
                .minimumShouldMatch(percentage)
                .queryName("at least " + percentage+ " term on name")
        )._toQuery();

        // Fuzzy match query with fuzziness and boost
        Query fuzzyMatchQuery = MatchQuery.of(mq -> mq
                .field(fieldName)
                .query(queryText)
                .fuzziness("2")
                .boost(1.4f)
                .queryName("fuzzy on name")
        )._toQuery();

        // Combine all queries in a bool "should" clause
        Query boolQuery = BoolQuery.of(bq -> bq
                //.should(matchPhraseQuery)
                //.should(matchQueryAllTerms)
                //.should(matchQueryOneTerm)
                //.should(fuzzyMatchQuery)
                .should(matchQuery75Term)

        )._toQuery();

        SearchRequest searchRequest = SearchRequest.of(sr -> sr
                .index("trade_names6")  // Replace with your index name
                .query(boolQuery)
        );
        log.info ("searchRequest query2 " + boolQuery);
        return searchRequest;
    }
}