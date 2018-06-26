package com.sksamuel.pulsar4s

import java.util.UUID

import com.sksamuel.exts.Logging
import org.apache.pulsar.client.api
import org.apache.pulsar.client.api.Schema

case class Topic(name: String)
case class Subscription(name: String)
object Subscription {
  def generate = Subscription(UUID.randomUUID.toString)
}

trait PulsarClient {
  def close(): Unit

  def producer[T](topic: Topic, config: ProducerConfig)(implicit schema: Schema[T]): Producer[T]
  def consumer[T](topic: Topic, config: ConsumerConfig)(implicit schema: Schema[T]): Consumer[T]
  def reader[T](topic: Topic, seek: MessageId, config: ReaderConfig)(implicit schema: Schema[T]): Reader[T]
}

object PulsarClient {

  // todo accept a PulsarClientUri object which has options
  def apply(url: String): PulsarClient = new PulsarClient with Logging {

    // todo add config options
    val client: api.PulsarClient = org.apache.pulsar.client.api.PulsarClient.builder().serviceUrl(url).build()

    override def close(): Unit = client.close()

    override def producer[T](topic: Topic, config: ProducerConfig)(implicit schema: Schema[T]): Producer[T] = {
      logger.info(s"Creating producer on $topic with config $config")
      val builder = client.newProducer(schema)
      builder.topic(topic.name)
      config.encryptionKey.foreach(builder.addEncryptionKey)
      config.blockIfQueueFull.foreach(builder.blockIfQueueFull)
      config.compressionType.foreach(builder.compressionType)
      new Producer(builder.create())
    }

    override def consumer[T](topic: Topic, config: ConsumerConfig)(implicit schema: Schema[T]): Consumer[T] = {
      logger.info(s"Creating consumer on $topic with config $config")
      val builder = client.newConsumer(schema)
      builder.topic(topic.name)
      config.consumerName.foreach(builder.consumerName)
      config.readCompacted.foreach(builder.readCompacted)
      new Consumer(builder.subscribe())
    }

    override def reader[T](topic: Topic, seek: MessageId, config: ReaderConfig)(implicit schema: Schema[T]): Reader[T] = {
      logger.info(s"Creating read on $topic with config $config and seek $seek")
      val builder = client.newReader(schema)
      builder.topic(topic.name)
      config.reader.foreach(builder.readerName)
      config.receiverQueueSize.foreach(builder.receiverQueueSize)
      config.readCompacted.foreach(builder.readCompacted)
      new Reader(builder.create(), topic)
    }
  }
}