Proposed next steps (short-term, testable)

1) Add save-time DB integrity handling for Payment (done) and also instrument metrics/logging for duplicate webhooks.
2) Background embedding refresh endpoint (done). Consider converting to scheduled job or async worker.
3) Implement semantic search (Priority 4): build ingestion, chunking, embedding, and a search endpoint.
4) Simple chatbot (Priority 5): wire `OpenAiClient.chat` behind an authenticated endpoint and add context management.
5) Add migration scripts for schema changes (unique transactionId) and tests for migration.

I can implement 3) next (semantic search ingestion and endpoint). Alternatively I can harden logging/metrics for payments. Which would you prefer?