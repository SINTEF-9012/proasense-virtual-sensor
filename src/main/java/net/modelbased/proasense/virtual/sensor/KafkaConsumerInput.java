/**
 * Copyright (C) 2014-2015 SINTEF
 *
 *     Brian Elvesæter <brian.elvesater@sintef.no>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.modelbased.proasense.virtual.sensor;

import eu.proasense.internal.SimpleEvent;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

public class KafkaConsumerInput implements Runnable {
    private BlockingQueue<SimpleEvent> queue;
    private String zooKeeper;
    private String groupId;
    private String topic;

    private ConsumerConnector kafkaConsumer;


    public KafkaConsumerInput(BlockingQueue<SimpleEvent> queue, String zooKeeper, String groupId, String topic) {
        this.queue = queue;
        this.zooKeeper = zooKeeper;
        this.groupId = groupId;
        this.topic = topic;

        // Initialize Kafka consumer
        this.kafkaConsumer = createKafkaConsumer(zooKeeper, groupId);
    }


    public KafkaConsumerInput(BlockingQueue<SimpleEvent> queue, String zooKeeper, String groupId, String topic, Properties kafkaProperties) {
        this.queue = queue;
        this.zooKeeper = zooKeeper;
        this.groupId = groupId;
        this.topic = topic;

        // Initialize Kafka consumer
        this.kafkaConsumer = createKafkaConsumer(zooKeeper, groupId, kafkaProperties);
    }


    public void run() {
//        ConsumerConnector kafkaConsumer = createKafkaConsumer(zooKeeper, groupId);

        // Create topic map
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();;
        topicCountMap.put(topic, 1);

        // Consume message
        Map<String, List<KafkaStream<byte[], byte[]>>> streams = kafkaConsumer.createMessageStreams(topicCountMap);
        KafkaStream<byte[], byte[]> messageAndMetadatas = streams.get(topic).get(0);
        ConsumerIterator<byte[], byte[]> it = messageAndMetadatas.iterator();

        int cnt = 0;
        try {
            while (it.hasNext()) {
                cnt++;
                byte[] bytes = it.next().message();

                // Convert message to Apache Thrift struct
                TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());
                SimpleEvent event = new SimpleEvent();
                deserializer.deserialize((SimpleEvent)event, bytes);
            }
        } catch (ConsumerTimeoutException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        } catch (TException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        } finally {
            kafkaConsumer.commitOffsets();
            kafkaConsumer.shutdown();
        }
    }


    private static ConsumerConnector createKafkaConsumer(String zooKeeper, String groupId) {
        // Specify default consumer properties
        Properties props = new Properties();
        props.put("zookeeper.connect", zooKeeper);
        props.put("group.id", groupId);
        props.put("zookeeper.connection.timeout.ms", "1000000");
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");

        // Create the connection to the cluster
        ConsumerConfig config = new ConsumerConfig(props);
        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(config);

        return consumer;
    }


    private static ConsumerConnector createKafkaConsumer(String zooKeeper, String groupId, Properties kafkaProperties) {
        // Specify specific consumer properties
        Properties props = new Properties();
        props.put("zookeeper.connect", zooKeeper);
        props.put("group.id", groupId);
        props.put("zookeeper.connection.timeout.ms", kafkaProperties.getProperty("zookeeper.connection.timeout.ms"));
        props.put("zookeeper.session.timeout.ms", kafkaProperties.getProperty("zookeeper.session.timeout.ms"));
        props.put("zookeeper.sync.time.ms", kafkaProperties.getProperty("zookeeper.sync.time.ms"));
        props.put("auto.commit.interval.ms", kafkaProperties.getProperty("kafka.auto.commit.interval.ms"));

        // Create the connection to the cluster
        ConsumerConfig config = new ConsumerConfig(props);
        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(config);

        return consumer;
    }


}
