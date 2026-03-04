package com.github.ocaso1987.eater.demo;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** CSV 解析用例：多行、带引号与逗号内引号的 CSV 解析示例与断言。 */
class CsvParserTest {

    record CsvRow(String name, int age, String city, boolean active, String note) {}

    static Parser<String> csvQuotedField() {
        return ctx -> {
            exactString("\"").parse(ctx);
            CharSource s = (CharSource) ctx.getSource();
            StringBuilder sb = new StringBuilder();
            while (true) {
                int pos = ctx.currentPosition();
                if (s.remainingChars(pos) < 1) {
                    ParseException ex = new ParseException("unclosed quoted field");
                    ex.addContextValue("position", pos);
                    throw ex;
                }
                char c = s.readChar(pos);
                ctx.setCurrentPosition(pos + 1);
                if (c != '"') {
                    sb.append(c);
                    continue;
                }
                if (s.remainingChars(ctx.currentPosition()) < 1) break;
                int nextPos = ctx.currentPosition();
                char next = s.readChar(nextPos);
                if (next == '"') {
                    sb.append('"');
                    ctx.setCurrentPosition(nextPos + 1);
                } else {
                    break;
                }
            }
            return sb.toString();
        };
    }

    static Parser<String> csvUnquotedField() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            StringBuilder sb = new StringBuilder();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                int pos = ctx.currentPosition();
                char c = s.readChar(pos);
                if (c == ',' || c == '\n') {
                    break;
                }
                sb.append(c);
                ctx.setCurrentPosition(pos + 1);
            }
            return sb.toString().trim();
        };
    }

    static Parser<String> csvField() {
        return ctx -> {
            String q = optional(csvQuotedField()).parse(ctx);
            if (q != null) return q;
            return csvUnquotedField().parse(ctx);
        };
    }

    static Parser<String> csvFieldThenComma() {
        return map(many(csvField(), exactString(",")), list -> list.get(0));
    }

    static Parser<String> csvUnquotedFieldRestOfLine() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            StringBuilder sb = new StringBuilder();
            while (s.remainingChars(ctx.currentPosition()) >= 1) {
                int pos = ctx.currentPosition();
                char c = s.readChar(pos);
                if (c == '\n') break;
                sb.append(c);
                ctx.setCurrentPosition(pos + 1);
            }
            return sb.toString().trim();
        };
    }

    static Parser<String> csvLastField() {
        return ctx -> {
            String q = optional(csvQuotedField()).parse(ctx);
            String s = q != null ? q : csvUnquotedFieldRestOfLine().parse(ctx);
            CharSource src = (CharSource) ctx.getSource();
            if (src.remainingChars(ctx.currentPosition()) >= 1) {
                int p = ctx.currentPosition();
                src.readChar(p);
                ctx.setCurrentPosition(p + 1);
            }
            return s;
        };
    }

    static Parser<CsvRow> csvRow() {
        return map(
            many(
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvLastField()
            ),
            list -> new CsvRow(
                list.get(0),
                Integer.parseInt(list.get(1)),
                list.get(2),
                Boolean.parseBoolean(list.get(3)),
                list.get(4)
            )
        );
    }

    static <R> Parser<List<R>> manyUntilEnd(Parser<R> p) {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            List<R> list = new ArrayList<>();
            for (; ; ) {
                R result = optional(p).parse(ctx);
                if (result == null) break;
                list.add(result);
                if (s.remainingChars(ctx.currentPosition()) < 1) break;
            }
            return list;
        };
    }

    static Parser<Void> csvHeaderRow() {
        return map(
            many(
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvFieldThenComma(),
                csvLastField()
            ),
            list -> null
        );
    }

    static Parser<List<CsvRow>> csvFileParser() {
        return ctx -> {
            csvHeaderRow().parse(ctx);
            return manyUntilEnd(csvRow()).parse(ctx);
        };
    }

    @Test
    void csv_multilineWithQuotedAndCommaInField_parsesRows() throws ReadException, ParseException {
        String csv = """
            name,age,city,active,note
            Alice,30,"Beijing",true,"lives in capital"
            Bob,25,Shanghai,false,"says ""hello""\"
            "Carol",28,"New York, NY",true,"multi
            line note"
            """;

        ParseContext ctx = ParseContext.fromChars(csv);
        List<CsvRow> result = csvFileParser().parse(ctx);

        assertEquals(3, result.size());
        CsvRow alice = result.get(0);
        assertEquals("Alice", alice.name());
        assertEquals(30, alice.age());
        assertEquals("Beijing", alice.city());
        assertTrue(alice.active());
        assertEquals("lives in capital", alice.note());

        CsvRow bob = result.get(1);
        assertEquals("Bob", bob.name());
        assertEquals(25, bob.age());
        assertEquals("says \"hello\"", bob.note());

        CsvRow carol = result.get(2);
        assertEquals("Carol", carol.name());
        assertEquals("New York, NY", carol.city());
        assertTrue(carol.note().contains("multi"));
    }
}
