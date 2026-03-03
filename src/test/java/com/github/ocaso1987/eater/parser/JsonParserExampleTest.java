package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** JSON 解析用例：简单对象（键值对为双引号字符串）的解析示例与断言。 */
class JsonParserExampleTest {

    static Parser<String> quotedString() {
        return ctx -> {
            exactString("\"").parse(ctx);
            String value = charsUntil('"').parse(ctx);
            exactString("\"").parse(ctx);
            return value;
        };
    }

    @Test
    void json_simpleObjectKeyValues_parsesToMap() throws ReadException, ParseException {
        String json = "{\"name\":\"Alice\",\"age\":\"30\",\"city\":\"Beijing\"}";
        ParseContext ctx = ParseContext.fromChars(json);

        exactString("{").parse(ctx);
        Map<String, String> obj = new LinkedHashMap<>();
        Parser<String> keyParser = quotedString();
        Parser<String> valueParser = quotedString();

        boolean first = true;
        while (ctx.hasChars(1)) {
            if (!first) {
                if (optional(exactString(",")).parse(ctx) == null) break;
            }
            first = false;
            String key = keyParser.parse(ctx);
            exactString(":").parse(ctx);
            String value = valueParser.parse(ctx);
            obj.put(key, value);
        }
        exactString("}").parse(ctx);

        assertEquals(3, obj.size());
        assertEquals("Alice", obj.get("name"));
        assertEquals("30", obj.get("age"));
        assertEquals("Beijing", obj.get("city"));
    }
}
