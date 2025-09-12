package lk.ijse.spring.smartplantmanagementsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlUtils {

private static final Logger log = LoggerFactory.getLogger(SqlUtils.class);
    private static final Pattern FENCE = Pattern.compile("```sql\\s*(.*?)\\s*```", Pattern.DOTALL);

    public String extractSql(String llmText) {
        Matcher m = FENCE.matcher(llmText);
        if (m.find()) {
            String sql = m.group(1).trim();
            log.debug("Extracted SQL:\n{}", sql);
            return sql;
        }
        log.warn("No fences found; using raw LLM output");
        return llmText.trim();
    }

    public void validateSql(String sql) {
        String lower = sql.toLowerCase();
        if (lower.contains("drop ") || lower.contains("truncate ")
                || (lower.contains("delete ") && !lower.contains(" where "))) {
            throw new RuntimeException("Rejected destructive SQL");
        }
        List<String> allowed = List.of(
                "user","plant","location","sensor_data","weather_data",
                "optimal_conditions","post","comment","vote","refresh_token");
        boolean ok = allowed.stream().anyMatch(t -> lower.contains(" " + t) || lower.contains(t + " "));
        if (!ok) throw new RuntimeException("SQL references unknown tables");
    }
}
