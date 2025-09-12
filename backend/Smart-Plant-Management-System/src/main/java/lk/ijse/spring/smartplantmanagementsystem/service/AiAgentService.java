package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatResponse;
import lk.ijse.spring.smartplantmanagementsystem.util.SqlPromptBuilder;
import lk.ijse.spring.smartplantmanagementsystem.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiAgentService {
    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final OllamaClientService ollama;
    private final SqlPromptBuilder prompts;
    private final SqlUtils sqlUtils;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Value("${ai.model.sql}")
    private String sqlModel;

    @Value("${ai.model.answer}")
    private String answerModel;

    // Pattern to detect clauses referencing sensor_data.plant_id
    private static final Pattern SENSOR_CLAUSE =
            Pattern.compile("(?i)AND\\s*sensor_data\\.plant_id\\s+IS\\s+NOT\\s+NULL");

    @Async("aiExecutor")
    public CompletableFuture<AiChatResponse> handleAsync(
            String question,
            Long plantId,
            boolean sqlOnly,
            Long userId
    ) {
        log.info("AI request: '{}' [plantId={}, sqlOnly={}, userId={}]",
                question, plantId, sqlOnly, userId);

        // Fallback for greetings
        String t = question.trim();
        if (t.length() < 5 || t.matches("(?i)^(hi|hello|hey)\\b.*")) {
            log.info("Greeting fallback");
            return CompletableFuture.completedFuture(
                    new AiChatResponse(null, List.of(),
                            "Hi there! How can I help with your farm today?")
            );
        }

        String sql = null;
        List<Map<String, Object>> rows = List.of();
        String answer = null;

        try {
            // Prompt → SQL
            String sqlPrompt = prompts.buildSqlPrompt(question, plantId, userId);
            String llmSql    = ollama.generate(sqlModel, sqlPrompt);
            sql = sqlUtils.extractSql(llmSql);
            sqlUtils.validateSql(sql);
            log.info("Generated SQL:\n{}", sql);

            if (sqlOnly) {
                return CompletableFuture.completedFuture(
                        new AiChatResponse(sql, rows, "(SQL only mode)")
                );
            }

            // Execute, with unknown-column recovery
            try {
                log.info("Executing SQL");
                rows = jdbc.queryForList(sql);
            } catch (BadSqlGrammarException ex) {
                String msg = ex.getRootCause().getMessage();
                if (msg != null && msg.contains("sensor_data.plant_id")) {
                    log.warn("Stripping sensor_data clause due to unknown column");
                    sql = stripSensorClause(sql);
                    log.info("Rewritten SQL:\n{}", sql);
                    rows = jdbc.queryForList(sql);
                } else {
                    throw ex;
                }
            }

            log.info("Query returned {} rows", rows.size());

            // SQL → Answer
            String resultJson   = objectMapper.writeValueAsString(rows);
            String answerPrompt = prompts.buildAnswerPrompt(question, sql, resultJson);
            answer = ollama.generate(answerModel, answerPrompt).trim();
            log.info("Generated answer");

            return CompletableFuture.completedFuture(
                    new AiChatResponse(sql, rows, answer)
            );

        } catch (DataAccessException dae) {
            log.error("Database error", dae);
            return CompletableFuture.completedFuture(
                    new AiChatResponse(sql, rows,
                            "Sorry, I ran into a database issue processing that request.")
            );
        } catch (Exception ex) {
            log.error("AI processing failed", ex);
            return CompletableFuture.completedFuture(
                    new AiChatResponse(sql, rows,
                            "Sorry, I couldn't process that request. Please try again.")
            );
        }
    }

    /**
     * Remove any "AND sensor_data.plant_id IS NOT NULL" clauses.
     */
    private String stripSensorClause(String originalSql) {
        Matcher m = SENSOR_CLAUSE.matcher(originalSql);
        return m.replaceAll("");
    }
}

