package com.wmdb.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Jackson XSS 反序列化器
 * <p>
 * 在 JSON 反序列化期间，对所有的 String 属性执行危险标签过滤，防止通过 RequestBody 注入 XSS。
 * 注意：不直接使用 htmlEscape，以避免破坏正常的数据（如单引号、双引号等在正常业务数据中出现）。
 * </p>
 *
 * @author wm
 */
public class XssJacksonDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value != null) {
            return stripXSS(value);
        }
        return null;
    }

    private String stripXSS(String value) {
        if (value != null) {
            // 仅仅过滤掉最明显的危险 XSS 代码结构
            // 注意：因为这是一个数据库/SQL 治理平台，过度过滤会导致正常 SQL 语句被破坏。
            // 实际企业级应用建议在展示层（前端）使用 DOMPurify 或对纯文本进行完全 HTML 编码处理，
            // 这里我们只去掉最致命且无明显业务意义的 script 节点注入和常见事件
            value = value.replaceAll("\0", "");

            // Only remove script tags explicitly if they are standalone tags intended for XSS
            // Be very conservative to prevent breaking user's actual SQL strings
            Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");

            scriptPattern = Pattern.compile("javascript:alert\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");

            scriptPattern = Pattern.compile("onerror=(.*?)[ >]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll(" ");
        }
        return value;
    }
}
