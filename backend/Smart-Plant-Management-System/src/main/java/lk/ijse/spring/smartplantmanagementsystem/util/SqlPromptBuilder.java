package lk.ijse.spring.smartplantmanagementsystem.util;

import org.springframework.stereotype.Component;

@Component
public class SqlPromptBuilder {
    private final String schemaHelp = """
    You are an expert Text-to-SQL generator for MySQL 'spms'.
    Generate SAFE, syntactically correct MySQL. Use only these tables/columns 
    and these explicit JOIN rules.

    Tables & key relationships:
    - user(u): u.id, u.email, u.password, u.role
    - plant(p): p.id, p.scientific_name, p.common_name, p.score, p.image_path, 
                p.planted_date, p.user_id, p.location_id, p.optimal_conditions_id
      JOIN user(u)      ON p.user_id = u.id
      JOIN location(l)  ON p.location_id = l.id
      JOIN optimal_conditions(o)
                        ON p.optimal_conditions_id = o.id
    - sensor_data(s): s.id, s.timestamp, s.air_temperature, s.air_humidity, 
                      s.soil_moisture, s.light_intensity, s.plant_id
      JOIN plant(p)     ON s.plant_id = p.id
    - weather_data(w): w.id, w.timestamp, w.temperature, w.humidity, 
                       w.precipitation, w.wind_speed, w.wind_gusts, w.cloud_cover,
                       w.uv_index, w.evapotranspiration, w.pressure, w.location_id
      JOIN location(l)  ON w.location_id = l.id
    - optimal_conditions(o): o.id, o.plant_name, o.ideal_temperature, 
                             o.ideal_humidity, o.ideal_rainfall, o.soil_type, 
                             o.sunlight_exposure, o.days_to_harvest, 
                             o.yield_prediction_kg
    - post(pt): pt.id, pt.title, pt.content, pt.cover_image_path, pt.tags, 
                pt.up_votes, pt.down_votes, pt.user_id, pt.created_at, pt.updated_at
      JOIN user(u)      ON pt.user_id = u.id
    - comment(c): c.id, c.post_id, c.user_id, c.text, c.created_at
      JOIN post(pt)     ON c.post_id = pt.id
      JOIN user(u)      ON c.user_id = u.id
    - vote(v): v.id, v.post_id, v.user_id, v.type, v.created_at, v.updated_at
      JOIN post(pt)     ON v.post_id = pt.id
      JOIN user(u)      ON v.user_id = u.id
    - refresh_token(rt): rt.id, rt.user_id, rt.token, rt.expiry_date
      JOIN user(u)      ON rt.user_id = u.id

    Rules:
    - Always filter by p.user_id = ${currentUserId} if asking "my ..." 
      or referencing plants/posts.
    - If a plantId is provided, also filter by p.id = ${plantId}.
    - ⚠️ Always use table aliases (u, p, l, w, o, pt, c, v, rt).
    - ⚠️ Always qualify columns with their alias (e.g. p.id, w.timestamp).
    - ⚠️ Do NOT generate placeholders like :currentUserId or ?.
      Always inline numeric values directly (e.g., p.user_id = 1).
    - ⚠️ Avoid "IN (SELECT id …)" because 'id' is ambiguous.
      ✅ Instead use: column = (SELECT p.location_id … ORDER BY … LIMIT 1).
    - Use WHERE w.timestamp >= NOW() - INTERVAL 7 DAY for time windows.
    - Use COUNT(*), AVG(), MIN(), MAX() for aggregates.
    - Always LIMIT lists (e.g. LIMIT 10).
    - Use weather_data (w) for weather conditions; do not reference sensor_data when querying weather.
    - For tags: FIND_IN_SET(tag, REPLACE(pt.tags,' ','')).
    Return ONLY the SQL between ```sql``` fences; no extra text.
    """;

    public String buildSqlPrompt(String question, Long plantId, Long userId) {
        String context = """
        Context:
        - currentUserId = %d
        %s
        """.formatted(userId,
                plantId == null
                        ? ""
                        : ("- plantId = " + plantId + "\n"));

        String prompt = schemaHelp
                .replace("${currentUserId}", userId.toString())
                + "\n\n"
                + context
                + "User question:\n\"" + question + "\"\n\n"
                + "```sql\nSELECT ...\n```";

        return prompt;
    }

    public String buildAnswerPrompt(String question, String sql, String resultJson) {
        return """
        You are a helpful assistant. The user asked:
        "%s"

        Executed SQL:
        ```sql
        %s
        ```

        JSON result:
        %s

        Summarize the result in two short paragraphs.
        """.formatted(question, sql, resultJson);
    }
}