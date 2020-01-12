import java.util.Properties
import net.liftweb.json.{DefaultFormats, Extraction, compactRender, parse}
import org.apache.flink.api.common.functions.{FilterFunction, MapFunction, RuntimeContext}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, KafkaDeserializationSchema}
import org.apache.http.HttpHost
import sources.GitHubEvent
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import scalaj.http.Http

object ID2221Project {

  def main(args: Array[String]) : Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "localhost:9092")
    //properties.setProperty("group.id", "GithubEventsConsumer5")
    properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flink-github-consumer")

    val myConsumer = new FlinkKafkaConsumer[GitHubEvent]("github_events_7", new GitHubEventDeserializationSchema(), properties)
    myConsumer.setStartFromGroupOffsets() // the default behaviour

    val stream = env.addSource(myConsumer)

    val httpHosts = new java.util.ArrayList[HttpHost]
    httpHosts.add(new HttpHost("localhost", 9200, "http"))

    val esSinkBuilder = new ElasticsearchSink.Builder[GitHubEvent](
      httpHosts,
      new ElasticsearchSinkFunction[GitHubEvent] {
//        def createIndexRequest(element: GitHubEvent): IndexRequest = {
//          val json = new java.util.HashMap[String, String]
//          json.put("id", element.id)
//          json.put("type", element.`type`)
//          json.put("actor.id", element.actor.id)
//          json.put("actor.login", element.actor.login)
//          json.put("actor.url", element.actor.url)
//          json.put("repo.id", element.repo.id)
//          json.put("repo.name", element.repo.name)
//          json.put("repo.url", element.repo.url)
//          json.put("created_at", element.created_at.toString)
//          json.put("language", element.language)
//
//          Requests.indexRequest()
//            .index("githubtest3")
//            .`type`("events")
//            .source(json)
//        }

        override def process(element: GitHubEvent, runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
          implicit val formats = DefaultFormats
          Http("http://localhost:9200/github/events").header("Content-Type", "application/json").postData(compactRender(Extraction.decompose(element))).asString
          //requestIndexer.add(createIndexRequest(element))
        }
      }
    )

    // configuration for the bulk requests; this instructs the sink to emit after every element, otherwise they would be buffered
    esSinkBuilder.setBulkFlushMaxActions(1)

    stream.addSink(esSinkBuilder.build())
    env.execute
  }
}

class GitHubEventDeserializationSchema extends KafkaDeserializationSchema[GitHubEvent] {

  def isEndOfStream(event: GitHubEvent) = false

  def getProducedType: TypeInformation[GitHubEvent] = TypeInformation.of(classOf[GitHubEvent])

  override def deserialize(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): GitHubEvent = {
    implicit val formats = DefaultFormats
    val event = parse(new String(consumerRecord.value()).toString).extract[GitHubEvent]
    event
  }
}