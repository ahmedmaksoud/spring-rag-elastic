package ai.rag.elastic.chat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class ElasticsearchConfig {

    private RestClient restClient;
    private RestClientTransport transport;

    final String username = "elastic";  // Replace with your Elasticsearch username
    final String password = "elastic";

    @Bean
    public ElasticsearchClient elasticsearchClient() {


        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder =
                RestClient.builder(new HttpHost("localhost", 9200))
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        RestClient myRes = builder.build();
        RestClientTransport transport = new RestClientTransport(myRes, new JacksonJsonpMapper());



        // Initialize RestClient
        this.restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")
        ).build();

        // Initialize RestClientTransport with Jackson mapper
        this.transport = transport;

        // Return Elasticsearch client
        return  new ElasticsearchClient(transport);
    }

    @PreDestroy
    public void closeResources() {
        // Close the RestClient and Transport when the application shuts down
        try {
            if (this.transport != null) {
                this.transport.close();
            }
            if (this.restClient != null) {
                this.restClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
