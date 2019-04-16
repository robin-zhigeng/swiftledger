/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.higgschain.trust.evmcontract.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.higgschain.trust.evmcontract.vm.DataWord;
import com.higgschain.trust.evmcontract.vm.OpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

/**
 * The type Serializers.
 */
public final class Serializers {

    private static final Logger LOGGER = LoggerFactory.getLogger("vmtrace");

    /**
     * The type Data word serializer.
     */
    public static class DataWordSerializer extends JsonSerializer<DataWord> {

        @Override
        public void serialize(DataWord gas, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(gas.value().toString());
        }
    }

    /**
     * The type Byte array serializer.
     */
    public static class ByteArraySerializer extends JsonSerializer<byte[]> {

        @Override
        public void serialize(byte[] memory, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(Hex.toHexString(memory));
        }
    }

    /**
     * The type Op code serializer.
     */
    public static class OpCodeSerializer extends JsonSerializer<Byte> {

        @Override
        public void serialize(Byte op, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(OpCode.code(op).name());
        }
    }

    /**
     * Serialize fields only string.
     *
     * @param value  the value
     * @param pretty the pretty
     * @return the string
     */
    public static String serializeFieldsOnly(Object value, boolean pretty) {
        try {
            ObjectMapper mapper = createMapper(pretty);
            mapper.setVisibilityChecker(fieldsOnlyVisibilityChecker(mapper));

            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            LOGGER.error("JSON serialization error: ", e);
            return "{}";
        }
    }

    private static VisibilityChecker<?> fieldsOnlyVisibilityChecker(ObjectMapper mapper) {
        return mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
    }

    /**
     * Create mapper object mapper.
     *
     * @param pretty the pretty
     * @return the object mapper
     */
    public static ObjectMapper createMapper(boolean pretty) {
        ObjectMapper mapper = new ObjectMapper();
        if (pretty) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }
}
