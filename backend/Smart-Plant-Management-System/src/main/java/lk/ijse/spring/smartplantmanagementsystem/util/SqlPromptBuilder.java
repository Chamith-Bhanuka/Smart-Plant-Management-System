package lk.ijse.spring.smartplantmanagementsystem.util;

import org.springframework.stereotype.Component;

@Component
public class SqlPromptBuilder {

    public String schemaHelp() {
        return """
        You are an expert Text-to-SQL generator for a MySQL database named 'spms'.
        Generate SAFE, syntactically correct MySQL. Use only the provided tables/columns.

        Tables:
        - user(id, email, password, role)
        - plant(id, scientific_name, common_name, score, image_path, planted_date, user_id, location_id, optimal_conditions_id)
        - location(id, latitude, longitude)
        - sensor_data(id, air_humidity, air_temperature, light_intensity, soil_moisture, timestamp, plant_id)
        - weather_data(id,timestamp,temperature,humidity,precipitation,windSpeed,windGusts,cloudCover,uvIndex,evapotranspiration,pressure,location_id)
        - optimal_conditions(id,plantName,idealTemperature,idealHumidity,idealRainfall,soilType,sunlightExposure,daysToHarvest,yieldPredictionKg)
        - post(id,title,content,coverImagePath,tags,upVotes,downVotes,user_id,createdAt,updatedAt)
        - comment(id, post_id, user_id, text, created_at)
        - vote(id, post_id, user_id, type, created_at, updated_at)
        - refresh_token(id, user_id, token, expiry_date)

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

    Explain the answer clearly in one or two short paragraphs.\s
    If no rows, say so.\s
    If numeric aggregates, include the value with unit if evident (e.g., %% for soil_moisture).
   \s""".formatted(question, sql, resultJson);
    }

}
