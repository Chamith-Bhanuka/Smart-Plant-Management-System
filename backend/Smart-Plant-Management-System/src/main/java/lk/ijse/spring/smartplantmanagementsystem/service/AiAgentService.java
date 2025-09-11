package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatResponse;
import lk.ijse.spring.smartplantmanagementsystem.util.SqlPromptBuilder;
import lk.ijse.spring.smartplantmanagementsystem.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAgentService {
    private final OllamaClientService ollama;
    private final SqlPromptBuilder prompts;
    private final SqlUtils sqlUtils;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.model.sql:mistral}")
    private String sqlModel;

    @Value("${ai.model.answer:mistral}")
    private String answerModel;

    public AiChatResponse handle(String question, Long plantId, boolean sqlOnly) throws Exception {
        //generate sql
        String sqlPrompt = prompts.buildSqlPrompt(question, plantId);
        String llmSql = ollama.generate(sqlModel, sqlPrompt);
        String sql = sqlUtils.extractSql(llmSql);
        sqlUtils.validateSql(sql);

        if (sqlOnly) {
            return withSqlOnly(sql);
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        String resultJson = objectMapper.writeValueAsString(rows);
        String answerPrompt = prompts.buildAnswerPrompt(question, sql, resultJson);
        String answer = ollama.generate(answerModel, answerPrompt).trim();

        AiChatResponse response = new AiChatResponse();
        response.setSql(sql);
        response.setResult(rows);
        response.setAnswer(answer);
        return response;
    }

    private AiChatResponse withSqlOnly(String sql) {
        AiChatResponse resp = new AiChatResponse();
        resp.setSql(sql);
        resp.setResult(List.of());
        resp.setAnswer("(SQL only mode)");
        return resp;
    }
}
