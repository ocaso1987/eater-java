package com.github.ocaso1987.eater.demo;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** INI 解析用例：节 [section]、键=值（支持空格与空值）。 */
class IniParserTest {

    /** 跳过当前行剩余内容并消费换行符。 */
    static Parser<Void> skipRestOfLine() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                int pos = ctx.currentPosition();
                char c = s.readChar(pos);
                ctx.setCurrentPosition(pos + 1);
                if (c == '\n') return null;
            }
            return null;
        };
    }

    /** 跳过零个或多个空格或制表符。 */
    static Parser<Void> skipSpaces() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                int pos = ctx.currentPosition();
                char c = s.readChar(pos);
                if (c != ' ' && c != '\t') break;
                ctx.setCurrentPosition(pos + 1);
            }
            return null;
        };
    }

    /** 解析 key=value 行（key/value 两侧可有空格，value 可为空）；非键值行或空行返回 null。 */
    static Parser<Map.Entry<String, String>> keyValueLine() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            int start = ctx.currentPosition();
            skipSpaces().parse(ctx);
            if (s.remainingChars(ctx.currentPosition()) < 1) return null;
            int pos = ctx.currentPosition();
            char c = s.readChar(pos);
            if (c == '\n' || c == '[') {
                if (c == '\n') ctx.setCurrentPosition(pos + 1);
                return null;
            }
            StringBuilder key = new StringBuilder();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                pos = ctx.currentPosition();
                c = s.readChar(pos);
                if (c == '=' || c == '\n') break;
                key.append(c);
                ctx.setCurrentPosition(pos + 1);
            }
            String keyStr = key.toString().trim();
            if (keyStr.isEmpty()) {
                skipRestOfLine().parse(ctx);
                return null;
            }
            if (s.remainingChars(ctx.currentPosition()) < 1 || s.readChar(ctx.currentPosition()) != '=') {
                skipRestOfLine().parse(ctx);
                return null;
            }
            ctx.setCurrentPosition(ctx.currentPosition() + 1);
            skipSpaces().parse(ctx);
            StringBuilder value = new StringBuilder();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                pos = ctx.currentPosition();
                c = s.readChar(pos);
                if (c == '\n') break;
                value.append(c);
                ctx.setCurrentPosition(pos + 1);
            }
            if (s.remainingChars(ctx.currentPosition()) >= 1) ctx.setCurrentPosition(ctx.currentPosition() + 1);
            return Map.entry(keyStr, value.toString().trim());
        };
    }

    /** 解析节标题 [name]，返回 name；非节行返回 null。 */
    static Parser<String> sectionHeader() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            skipSpaces().parse(ctx);
            if (s.remainingChars(ctx.currentPosition()) < 2) return null;
            int pos = ctx.currentPosition();
            if (s.readChar(pos) != '[') return null;
            pos++;
            ctx.setCurrentPosition(pos);
            StringBuilder name = new StringBuilder();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                pos = ctx.currentPosition();
                char c = s.readChar(pos);
                if (c == ']') {
                    ctx.setCurrentPosition(pos + 1);
                    skipRestOfLine().parse(ctx);
                    return name.toString().trim();
                }
                if (c == '\n') break;
                name.append(c);
                ctx.setCurrentPosition(pos + 1);
            }
            return null;
        };
    }

    /** 解析整份 INI，返回 节名 -> (键 -> 值)；全局键归在 "" 下。 */
    static Parser<Map<String, Map<String, String>>> iniFile() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            Map<String, String> current = new LinkedHashMap<>();
            sections.put("", current);

            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                String section = optional(sectionHeader()).parse(ctx);
                if (section != null) {
                    current = new LinkedHashMap<>();
                    sections.put(section, current);
                    continue;
                }
                Map.Entry<String, String> kv = optional(keyValueLine()).parse(ctx);
                if (kv != null) {
                    current.put(kv.getKey(), kv.getValue());
                    continue;
                }
                skipSpaces().parse(ctx);
                if (s.remainingChars(ctx.currentPosition()) >= 1) {
                    char next = s.readChar(ctx.currentPosition());
                    if (next == '\n') {
                        ctx.setCurrentPosition(ctx.currentPosition() + 1);
                    } else if (next != '[') {
                        skipRestOfLine().parse(ctx);
                    }
                }
            }
            return sections;
        };
    }

    @Test
    void ini_multipleSectionsAndGlobalKeys_parsesToMap() throws ReadException, ParseException {
        String ini = """
            username = noha
            password = plain_text
            salt = NaCl

            [server_1]
            interface=eth0
            ip=127.0.0.1
            document_root=/var/www/example.org

            [empty_section]

            [second_server]
            document_root=/var/www/example.com
            ip=
            interface=eth1
            """;

        ParseContext ctx = ParseContext.fromChars(ini);
        Map<String, Map<String, String>> sections = iniFile().parse(ctx);

        Map<String, String> global = sections.get("");
        assertNotNull(global);
        assertEquals("noha", global.get("username"));
        assertEquals("plain_text", global.get("password"));
        assertEquals("NaCl", global.get("salt"));

        Map<String, String> server1 = sections.get("server_1");
        assertNotNull(server1);
        assertEquals("eth0", server1.get("interface"));
        assertEquals("127.0.0.1", server1.get("ip"));
        assertEquals("/var/www/example.org", server1.get("document_root"));

        assertTrue(sections.containsKey("empty_section"));
        assertTrue(sections.get("empty_section").isEmpty());

        Map<String, String> second = sections.get("second_server");
        assertNotNull(second);
        assertEquals("/var/www/example.com", second.get("document_root"));
        assertEquals("", second.get("ip"));
        assertEquals("eth1", second.get("interface"));
    }
}
