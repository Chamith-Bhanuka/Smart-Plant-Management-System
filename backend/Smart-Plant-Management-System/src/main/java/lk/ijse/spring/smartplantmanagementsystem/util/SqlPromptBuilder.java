package lk.ijse.spring.smartplantmanagementsystem.util;

import org.springframework.stereotype.Component;

@Component
public class SqlPromptBuilder {

    public String schemaHelp() {
        return """
        You are an expert Text-to-SQL generator for a MySQL database named 'smartfarm'.
        Generate SAFE, syntactically correct MySQL. Use only the provided tables/columns.

        Tables:
        - user(id, email, full_name)
        - plant(id, user_id, name, species, location_id, planted_date)
        - location(id, name, description)
        - sensor_data(id, plant_id, timestamp, temperature, humidity, soil_moisture, light_intensity)
        - weather_data(id, location_id, timestamp, temperature, humidity, wind_speed, precipitation, uv_index, cloud_cover, pressure, et0)
        - optimal_conditions(id, species, min_temp, max_temp, min_humidity, max_humidity, min_soil_moisture, max_soil_moisture, min_light, max_light)
        - post(id, user_id, title, content, tags, up_votes, down_votes, created_at, updated_at)
        - comment(id, post_id, user_id, text, created_at)
        - vote(id, post_id, user_id, type, created_at)
        - refresh_token(id, user_id, token, expires_at)

        Rules:
        - Prefer WHERE timestamp >= NOW() - INTERVAL 7 DAY for "last week".
        - Use COUNT(*), AVG(col), MIN/MAX as needed.
        - Use LIMIT for lists unless count requested.
        - For tags in post.tags (comma string), use LIKE '%,tag,%' or find_in_set(tag, replace(tags, ' ', '')) if normalized.
        - For votes, net_votes = up_votes - down_votes.

        Return ONLY the SQL between ```sql and ``` fences. No commentary.
        """;
    }

    public String buildSqlPrompt(String question, Long plantId) {
        String plantContext = plantId == null ? "" : """
        Context:
        - Focus queries on plant_id = %d when relevant.
        """.formatted(plantId);

        return """
        %s

        %s

        User question:
        "%s"

        Return MySQL in:
        ```sql
        SELECT ...
        ```
        """.formatted(schemaHelp(), plantContext, question);
    }

    public String buildAnswerPrompt(String question, String sql, String resultJson) {
        return """
        You are a helpful assistant. The user asked:
        "%s"

        We executed this SQL:
        ```sql
        %s
        ```

        JSON result:
        %s

        Explain the answer clearly in one or two short paragraphs. If no rows, say so. If numeric aggregates, include the value with unit if evident (e.g., % for soil_moisture).
        """.formatted(question, sql, resultJson);
    }
}
