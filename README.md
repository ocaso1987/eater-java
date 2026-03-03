# Eater-Java

字节流 / 字符流的组合子式解析库。通过 `Parser<R>` 从 `ParseContext` 中消费输入并返回结构化结果，支持按字节、按字符、可选、多选、重复、映射等组合子，适用于协议解析、简单 CSV/JSON 等场景。

## 特性

- **双模式**：同一套组合子可作用于字节流（`ByteBuffer`）或字符流（`CharSequence`），创建上下文时选定模式。
- **组合子 API**：`optional`、`many`、`choose`、`repeat`、`map`、`peek` 等，便于组合出复杂解析逻辑。
- **位置与回退**：上下文维护当前读取位置，解析失败时可 `restorePosition` 回退（如 `optional` 在 `ReadException` 时回退并返回 `null`）。
- **异常区分**：`ReadException` 表示数据不足/越界等读取失败，`ParseException` 表示格式或语义解析失败；均支持 `addContextValue` 携带诊断信息。

## 要求

- Java 17+

## 安装

```bash
git clone <repo-url>
cd eater-java
mvn install
```

若作为依赖使用，在 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>com.github.ocaso1987.eater</groupId>
    <artifactId>eater-java</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 快速开始

### 字符解析

```java
import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.exception.ParseException;

import static com.github.ocaso1987.eater.Parsers.*;

// 创建字符模式上下文
ParseContext ctx = ParseContext.fromChars("hello world");

// 组合解析器并执行
String first = exactString("hello").parse(ctx);   // "hello"，位置推进到 5
String rest = exactString(" world").parse(ctx);   // " world"

// 或一次组合多个
Parser<List<String>> p = many(exactString("hello"), exactString(" world"));
List<String> list = p.parse(ctx);
```

### 字节解析

```java
byte[] data = "Hi".getBytes(StandardCharsets.UTF_8);
ParseContext ctx = ParseContext.fromBytes(data);

byte[] raw = bytes(2).parse(ctx);           // 读 2 字节
String s = bytesAsUtf8(2).parse(ctx);      // 读 2 字节并按 UTF-8 解码为字符串

// 魔数 + 长度 + 负载 示例
byte[] magic = exactBytes("DEMO".getBytes(StandardCharsets.UTF_8)).parse(ctx);
byte[] lenBytes = bytes(4).parse(ctx);
int len = /* 按小端解析 lenBytes */;
byte[] payload = bytes(len).parse(ctx);
```

### 可选与多选

```java
// 成功则消费并返回结果；仅当 ReadException 时回退并返回 null
String maybe = optional(exactString("prefix")).parse(ctx);

// 依次尝试，返回第一个成功结果；仅对 ReadException 尝试下一个，ParseException 直接抛出
Parser<String> p = choose(exactString("a"), exactString("ab"), exactString("abc"));
String matched = p.parse(ctx);

// 将解析器重复 n 次
List<String> three = repeat(map(oneChar(), c -> String.valueOf(c)), 3).parse(ctx);

// 对结果做映射
Parser<Integer> num = map(chars(3), s -> Integer.parseInt(s));
```

## API 概览

### 核心类型

| 类型 | 说明 |
|------|------|
| `Parser<R>` | 解析器接口：`R parse(ParseContext context)`，消费输入并推进位置。 |
| `ParseContext` | 解析上下文：持有数据源与当前位置，提供 `readByte`/`readChars`、`position`、`restorePosition` 等。 |
| `ParseSource` | 数据源封装（ByteBuffer 或 CharSequence），按索引读取，不持有位置。 |

### 创建上下文

- `ParseContext.fromBytes(byte[] data)` / `fromBytes(data, offset, length)` / `fromByteBuffer(ByteBuffer)`
- `ParseContext.fromString(CharSequence text)` / `fromString(text, Charset)`（字符串编码为字节后按字节模式解析）
- `ParseContext.fromChars(CharSequence text)`（字符模式，不经编码）

### Parsers 门面（建议 `import static Parsers.*`）

**字节**：`bytes(n)`、`oneByte()`、`exactBytes(expected)`、`bytesUntil(delimiter)`、`bytesAsString(n, charset)`、`bytesAsUtf8(n)`  

**字符**：`chars(n)`、`oneChar()`、`exactString(expected)`、`charsUntil(delimiter)`  

**组合**：`optional(p)`、`many(p)`、`many(p1, p2, ...)`、`choose(p1, p2, ...)`、`repeat(p, n)`、`map(p, f)`  

**通用**：`peek(p)`（执行解析但不消费输入）

### 异常

- `ReadException`：数据不足、越界等；可 `addContextValue(label, value)`，`getMessage()` 会包含上下文。
- `ParseException`：格式或语义错误；同样支持上下文。
- 字节/字符模式混用（如在字符模式下调 `readByte()`）会抛出 `UnsupportedOperationException`。

## 测试与示例

- **基础测试**：`ParsersTest` 覆盖各组合子与上下文行为。
- **CSV 示例**：`CsvParserExampleTest` 中多行、带引号与逗号内内容的 CSV 解析。
- **JSON 示例**：`JsonParserExampleTest` 中简单对象（双引号字符串键值对）的解析。

```bash
mvn test
```

## 包结构

```
com.github.ocaso1987.eater
├── Parser.java           # 解析器接口
├── Parsers.java          # 门面：字节/字符/组合/通用解析器
├── exception
│   ├── ReadException     # 读取失败
│   ├── ParseException    # 解析/格式失败
│   └── ContextAwareException  # 带上下文的异常基类
└── context
    ├── ParseContext      # 解析上下文（位置 + 顺序读）
    └── ParseSource       # 数据源（字节/字符）
```

解析器实现位于 `eater.parser` 包（`ByteParsers`、`CharParsers`、`ComboParsers`、`CommonParsers`），一般通过 `Parsers` 门面使用即可。

## 许可证

见项目仓库或根目录许可证文件。
