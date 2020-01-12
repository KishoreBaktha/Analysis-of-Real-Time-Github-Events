package sources

import java.io.StringWriter
import java.lang.Exception
import java.util
import java.util.{Date, Properties}

import com.fasterxml.jackson.databind.ObjectMapper
import net.liftweb.json.DefaultFormats
import net.liftweb.json._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.joda.time.DateTime
import scalaj.http.{Http, HttpResponse}

object GitHubEventsProducer extends App {

  def getEvents: Array[GitHubBaseEvent] = {
    implicit val formats = DefaultFormats
    val response: HttpResponse[String] = Http("https://api.github.com/events?client_id=4d2dad1eab67aa5c6554&client_secret=779e189e0f4ef2e9310f8af0f3bd982e4a83d8e8").asString
    parse(response.body).extract[Array[GitHubBaseEvent]]
  }

  def getLanguages(repoUrl: String): String = {
    try {
      implicit val formats = DefaultFormats
      val response: HttpResponse[String] = Http(repoUrl + "/languages?client_id=ee0381f9d538c64b62f1&client_secret=48058542bfdb18fe92710b115a057cdff57da8d0").asString
      val languages = parse(response.body).extract[Map[String, Int]].keys.toArray
      if (languages.size > 0) languages(0) else ""
    }
    catch {
      case x: Exception => {
        ""
      }
    }
  }

  def getLocation(actorLoginName: String): String = {
    implicit val formats = DefaultFormats
    val response: HttpResponse[String] = Http("https://api.github.com/users/" + actorLoginName + "?client_id=ee0381f9d538c64b62f1&client_secret=48058542bfdb18fe92710b115a057cdff57da8d0").asString
    val user = parse(response.body).extract[GitHubUser]
    user.location
  }

  def getLocationGeohash(location: String): String = {

    if (location == "" || location == null)
      return null
    try {
      implicit val formats = DefaultFormats
      val response: HttpResponse[String] = Http("https://api.opencagedata.com/geocode/v1/json?q=" + location.split(",")(0) + "&key=44a55adea14f4e6ba1702bd5389d6f5a").asString
      val geoInfoResult = parse(response.body).extract[GeoInfoResponse]
      if (geoInfoResult != null && geoInfoResult.results != null && geoInfoResult.results.length > 0)
        geoInfoResult.results(0).annotations.geohash
      else
        null
    }
    catch {
      case x: Exception => {
        null
      }
    }

  }

  val topic = "github_events_7"
  val brokers = "localhost:9092"

  val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  props.put(ProducerConfig.CLIENT_ID_CONFIG, "GithubEventProducer")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "sources.GitHubEventSerializer")
  val producer = new KafkaProducer[String, GitHubEvent](props)
  while(true) {
    val events = getEvents
    for (event <- events) {
      val language = getLanguages(event.repo.url)
      val hour = new DateTime(event.created_at).hourOfDay().get()
      val location = getLocation(event.actor.login)
      val location_geohash = getLocationGeohash(location)

      val newEvent = new GitHubEvent(event.id, event.`type`,  event.actor, event.repo, event.payload, event.created_at, language, hour, location, location_geohash)
      val data = new ProducerRecord[String, GitHubEvent](topic, newEvent.id, newEvent)
      producer.send(data)
      print(data + "\n")
    }

    Thread.sleep(30000)
  }


  producer.close()
}

class GitHubEventSerializer extends org.apache.kafka.common.serialization.Serializer[GitHubEvent] {
  override def serialize(s: String, t: GitHubEvent): Array[Byte] = {
    if(t==null)
      null
    else
    {
      implicit val formats = DefaultFormats
      compactRender(Extraction.decompose(t)).getBytes
    }
  }
  override def close(): Unit = {
  }
}

class GitHubEventDeserializer extends org.apache.kafka.common.serialization.Deserializer[GitHubEvent] {
  override def deserialize(s: String, bytes: Array[Byte]): GitHubEvent = {
    val mapper = new ObjectMapper()
    val event = mapper.readValue(bytes, classOf[GitHubEvent])
    event
  }
  override def close(): Unit = {
  }
}

class GitHubBaseEvent(val id: String, val `type`: String, val actor: GitHubActor, val repo: GitHubRepo, val payload: GitHubEventPayload, val created_at: Date)
class GitHubEvent(val id: String, val `type`: String, val actor: GitHubActor, val repo: GitHubRepo, val payload: GitHubEventPayload, val created_at: Date,
                  var language: String, var hour_of_day: Int, var location: String, var geohash: String)

class GitHubEventPayload()

case class GitHubActor(id: String, login: String, display_login: String, url: String, avatar_url: String)
case class GitHubRepo(id: String, name: String, url: String)
case class GitHubUser(id: String, location: String)

case class GeoInfoResponse(results: Array[GeoInfoAnnotation])
case class GeoInfoAnnotation(annotations: GeoInfo)
case class GeoInfo(geohash: String)
