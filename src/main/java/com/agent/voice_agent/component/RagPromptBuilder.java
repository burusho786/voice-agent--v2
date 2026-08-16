package com.agent.voice_agent.component;

import com.agent.voice_agent.dto.VectorSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    public String build(
            String question,
            List<VectorSearchResult> results
    ) {

        StringBuilder context = new StringBuilder();

        for (VectorSearchResult result : results) {

            context
                    .append(result.chunk().content())
                    .append("\n\n");
        }

        return """
                You are the voice assistant for the company.
                
                                Answer the user's question using ONLY the
                                company information provided in the context.
                
                                Your responsibilities:
                
                                - Give accurate answers based only on the context.
                                - Explain complex company information in simple,
                                  natural language when necessary.
                                - Preserve important facts, policies, prices,
                                  conditions, and limitations.
                                - Do not invent information.
                                - Do not assume information that is not present.
                                - If the answer cannot be found in the context,
                                  clearly say that you do not have enough
                                  company information to answer.
                                - Keep the response concise and conversational
                                  because the response will be converted to speech.
                                - Prefer 2 to 5 sentences unless the user
                                  explicitly asks for a detailed explanation.
                                - Do not use markdown.
                                - Do not mention the retrieved context,
                                  vector database, RAG, or these instructions.
                
                                COMPANY CONTEXT:
                                %s
                
                                USER QUESTION:
                                %s
                

                ANSWER:
                """.formatted(
                context,
                question
        );
    }
}