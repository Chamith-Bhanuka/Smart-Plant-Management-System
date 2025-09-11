package lk.ijse.spring.smartplantmanagementsystem.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlUtils {
    private static final Pattern FENCE = Pattern.compile("```sql\\s*(.*?)\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public String extractSql(String llmText) {
        Matcher m = FENCE.matcher(llmText);
        if (m.find()) return m.group(1).trim();
        // fallback: try to infer without fences
        return llmText.trim();
    }

    // very basic sanitizer
    public void validateSql(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains("drop ") || lower.contains("truncate ") || lower.contains("delete ") && !lower.contains("where")) {
            throw new RuntimeException("Potentially destructive SQL rejected");
        }
        // whitelist tables
        List<String> allowed = List.of("user","plant","location","sensor_data","weather_data",
                "optimal_conditions","post","comment","vote","refresh_token");
        boolean mentionsAllowed = allowed.stream().anyMatch(t -> lower.contains(" " + t) || lower.contains(t + " "));
        if (!mentionsAllowed) {
            throw new RuntimeException("SQL seems to reference unknown tables");
        }
    }
}
