package com.gameplatform.notificationservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gameplatform.notificationservice.domain.event.HeroBugReportCreatedEvent;
import com.gameplatform.notificationservice.domain.event.JointPurchaseParticipantsEmailRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private ObjectMapper kafkaObjectMapper() {
        ObjectMapper kafkaObjectMapper = new ObjectMapper();
        kafkaObjectMapper.registerModule(new JavaTimeModule());
        kafkaObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return kafkaObjectMapper;
    }

    @Bean
    public ConsumerFactory<String, JointPurchaseParticipantsEmailRequestedEvent>
    jointPurchaseParticipantsEmailRequestedConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<JointPurchaseParticipantsEmailRequestedEvent> valueDeserializer =
                new JsonDeserializer<>(JointPurchaseParticipantsEmailRequestedEvent.class, kafkaObjectMapper(), false);

        valueDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConsumerFactory<String, HeroBugReportCreatedEvent>
    heroBugReportCreatedConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<HeroBugReportCreatedEvent> valueDeserializer =
                new JsonDeserializer<>(HeroBugReportCreatedEvent.class, kafkaObjectMapper(), false);

        valueDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "jointPurchaseParticipantsEmailRequestedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, JointPurchaseParticipantsEmailRequestedEvent>
    jointPurchaseParticipantsEmailRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, JointPurchaseParticipantsEmailRequestedEvent>
                    jointPurchaseParticipantsEmailRequestedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, JointPurchaseParticipantsEmailRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(jointPurchaseParticipantsEmailRequestedConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean(name = "heroBugReportCreatedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HeroBugReportCreatedEvent>
    heroBugReportCreatedKafkaListenerContainerFactory(
            ConsumerFactory<String, HeroBugReportCreatedEvent> heroBugReportCreatedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, HeroBugReportCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(heroBugReportCreatedConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
