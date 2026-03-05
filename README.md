# Eater-Java

字节流 / 字符流的组合子式解析库。通过 `Parser<R>` 从 `ParseContext` 消费输入并返回结构化结果，支持按字节、按字符、可选、多选、重复、映射等组合子，适用于协议解析、简单 CSV/JSON 等场景。

## 特性

- **多源抽象**：数据源为泛型抽象类 `ParseSource<T>`，仅暴露 `getSource()`；实现类 `ByteSource`、`CharSource`、`ValueSource` 各自提供字节/字符/对象的读方法。
- **组合子 API**：`optional`、`many`、`choose`、`repeat`、`map`、`peek` 等，便于组合复杂解析逻辑。
- **位置与回退**：上下文持有 `currentReadPosition()` / `currentWritePosition()`，解析失败时可通过 `setCurrentReadPosition(int)` 回退；读取由解析器通过 `getSource()` 获取对应源类型后调用其方法。
- **对象源与 Scope**：`fromObject(root)` 创建对象源并设置初始 `currentInputScope`；通过 `enterInputScope(childTarget)` / `exitInputScope()` 及对应的 output 方法进入/退出子属性解析，用于层级对象解析。
- **异常区分**：`ReadException` 表示数据不足/越界等读取失败，`ParseException` 表示格式或语义解析失败；均支持 `addContextValue` 携带诊断信息。

## 要求

- Java 17+

## 安装

```bash
git clone <repo-url>
cd eater-java
mvn install
```

作为依赖使用：

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

ParseContext ctx = ParseContext.fromChars("hello world");

String first = str_expect("hello").parse(ctx);   // "hello"
String rest = str_expect(" world").parse(ctx);   // " world"

Parser<List<String>> p = many(str_expect("hello"), str_expect(" world"));
List<String> list = p.parse(ctx);
```

### 字节解析

```java
byte[] data = "Hi".getBytes(StandardCharsets.UTF_8);
ParseContext ctx = ParseContext.fromBytes(data);

byte[] raw = byte_n(2).parse(ctx);
String s = byte_utf8(2).parse(ctx);

// 魔数 + 长度 + 负载
byte[] magic = byte_expect("DEMO".getBytes(StandardCharsets.UTF_8)).parse(ctx);
byte[] lenBytes = byte_n(4).parse(ctx);
int len = /* 小端解析 lenBytes */;
byte[] payload = byte_n(len).parse(ctx);
```

### 可选与多选

```java
String maybe = optional(str_expect("prefix")).parse(ctx);

Parser<String> p = choose(str_expect("a"), str_expect("ab"), str_expect("abc"));
String matched = p.parse(ctx);

List<String> three = repeat(map(char_one(), c -> String.valueOf(c)), 3).parse(ctx);
Parser<Integer> num = map(str_n(3), s -> Integer.parseInt(s));
```

## API 概览

### 核心类型

| 类型 | 说明 |
|------|------|
| `Parser<R>` | 解析器接口：`R parse(ParseContext context)`，消费输入并推进位置。 |
| `ParseContext` | 解析上下文：持有 `getSource()`、`currentReadPosition()` / `setCurrentReadPosition(int)`、`currentWritePosition()` / `setCurrentWritePosition(int)`、`getCurrentInputScope()` / `enterInputScope(Object)` / `exitInputScope()` 及对应的 output 方法；不提供源的读取方法。 |
| `ParseScope` | 解析范围：`getParent()`、`getTarget()`、`enter(childTarget)`、`exit()`、`isRoot()`；用于对象源层级解析。 |
| `ParseSource<T>` | 数据源泛型抽象类，仅暴露 `getSource()`。 |
| `ByteSource` | 字节源：getLength、validatePosition、readByte、readBytes、remainingBytes。 |
| `CharSource` | 字符源：getLength、validatePosition、readChar、readChars、remainingChars。 |
| `ValueSource` | 值源：持有一个 Object，OGNL `getValue(expression)` / `getValue(expression, Class<R>)`。 |

### 创建上下文

- **字节源**：`fromBytes(byte[])`、`fromBytes(byte[], offset, length)`、`fromByteBuffer(ByteBuffer)`、`fromEncodedBytes(text, Charset)`、`fromEncodedBytesUtf8(text)`
- **字符源**：`fromString(CharSequence)`（推荐）、`fromChars(CharSequence)`
- **对象源**：`fromObject(root)`；自动设置以 root 为 target 的根 `currentInputScope`；通过 `(ValueSource) ctx.getSource()` 调用 `getValue(...)`

### Parsers 门面（建议 `import static com.github.ocaso1987.eater.Parsers.*`）

- **字节**：`byte_n(n)`、`byte_one()`、`byte_expect(expected)`、`byte_until(delimiter)`、`byte_asString(n, charset)`、`byte_utf8(n)`
- **字符/字符串**：`str_n(n)`、`char_one()`、`str_expect(expected)`、`str_until(delimiter)`
- **值源**：`val(expression)`、`val(expression, Class<R>)`
- **组合**：`optional(p)`、`many(p)`、`many(p1, p2, ...)`、`choose(p1, p2, ...)`、`repeat(p, n)`、`map(p, f)`
- **通用**：`peek(p)`（解析但不消费输入）

### 异常

- `ReadException`：数据不足、越界等；可 `addContextValue`，`getMessage()` 含上下文。
- `ParseException`：格式或语义错误；同样支持上下文。
- 解析器需将 `ctx.getSource()` 转为 `ByteSource` 或 `CharSource` 后调用读方法；类型不匹配时强转会抛出 `ClassCastException`。

## 测试与示例

- **测试包** `com.github.ocaso1987.eater`：`ParsersTest`（组合子与上下文）、`ParseContextTest`、`ValueTargetTest`、`ExceptionCoverageTest`、`SourceCoverageTest`、`SinkCoverageTest`（ByteSink/CharSink 与写解析器）。
- **Demo 包** `com.github.ocaso1987.eater.demo`：`CsvParserTest`（多行、带引号与逗号内内容的 CSV）、`JsonParserTest`（简单对象键值对）。

```bash
mvn test
```

**覆盖率**：JaCoCo 行覆盖率要求 ≥ 90%，在 `mvn verify` 阶段校验；报告路径 `target/site/jacoco/index.html`。

## 包结构

```
com.github.ocaso1987.eater
├── Parser.java              # 解析器接口
├── Parsers.java              # 门面：字节/字符/组合/通用解析器
├── exception/
│   ├── ReadException         # 读取失败
│   ├── ParseException        # 解析/格式失败
│   └── ContextAwareException # 带上下文的异常基类
├── context/
│   ├── ParseContext          # 解析上下文（source + position + scope，不提供读方法）
│   ├── ParseScope            # 解析范围（parent + target，用于对象源层级）
│   ├── ParseSource           # 数据源泛型抽象（仅 getSource）
│   ├── ByteSource            # 字节源
│   ├── CharSource            # 字符源
│   └── ValueSource         # 对象源（OGNL getValue）
└── parser/                   # 解析器实现，一般通过 Parsers 门面使用
    ├── ByteSourceParsers
    ├── CharSourceParsers
    ├── ComboParsers
    └── CommonParsers
```

## 项目页（GitHub Pages）

文档站位于 **docs/**，包含主页、介绍（下载/开始）、指南（配置/异常）、样例（CSV/JSON）、API。在仓库 **Settings → Pages** 中 Source 选 **Deploy from a branch**、Folder 选 **/docs** 即可发布。详见 `docs/README.md`。

## 许可证

见项目仓库或根目录许可证文件。
