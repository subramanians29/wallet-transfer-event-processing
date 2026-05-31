package com.wallet.transfer.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wallet.transfer.domain.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;


@Configuration
public class JacksonConfig {

    @Bean
    public Module moneyModule(){
        SimpleModule module = new SimpleModule("MoneyModule");
        module.addSerializer(Money.class, new MoneySerializer());
        module.addDeserializer(Money.class, new MoneyDeserializer());
        return module;
    }

    static class MoneySerializer extends JsonSerializer<Money> {
        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("amount", value.getAmount().toPlainString());
            gen.writeStringField("currency", value.getCurrency().getCurrencyCode());
            gen.writeEndObject();
        }
    }

    static class MoneyDeserializer extends JsonDeserializer<Money> {
        @Override
        public Money deserialize(JsonParser p, DeserializationContext serializers) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            Currency currency = Currency.getInstance(node.get("currency").asText());
            return Money.of(amount, currency);
        }
    }

}
