package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** CSV 解析用例：多行、带引号与逗号内引号的 CSV 解析示例与断言。 */
class CsvParserExampleTest {

    record CsvRow(String name, int age, String city, boolean active, String note) {}

    static Parser<String> csvQuotedField() {
        return ctx -> {
            exactString("\"").parse(ctx);
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (!ctx.hasChars(1)) {
                    ParseException ex = new ParseException("unclosed quoted field");
                    ex.addContextValue("position", ctx.position());
                    throw ex;
                }
                char c = ctx.readChar();
                if (c != '"') {
                    sb.append(c);
                    continue;
                }
                int pos = ctx.position();
                if (!ctx.hasChars(1)) break;
                char next = ctx.readChar();
                if (next == '"') {
                    sb.append('"');
                } else {
                    ctx.restorePosition(pos);
                    break;
                }
            }
            return sb.toString();
        };
    }

    static Parser<String> csvUnquotedField() {
        return ctx -> {
            StringBuilder sb = new StringBuilder();
            while (ctx.hasChars(1)) {
                char c = ctx.readChar();
                if (c == ',' || c == '\n') {
                    ctx.restorePosition(ctx.position() - 1);
                    break;
                }
                sb.append(c);
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
            StringBuilder sb = new StringBuilder();
            while (ctx.hasChars(1)) {
                char c = ctx.readChar();
                if (c == '\n') {
                    ctx.restorePosition(ctx.position() - 1);
                    break;
                }
                sb.append(c);
            }
            return sb.toString().trim();
        };
    }

    static Parser<String> csvLastField() {
        return ctx -> {
            String q = optional(csvQuotedField()).parse(ctx);
            String s = q != null ? q : csvUnquotedFieldRestOfLine().parse(ctx);
            if (ctx.hasChars(1)) ctx.readChar();
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
            List<R> list = new ArrayList<>();
            for (; ; ) {
                R result = optional(p).parse(ctx);
                if (result == null) break;
                list.add(result);
                if (!ctx.hasChars(1)) break;
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
