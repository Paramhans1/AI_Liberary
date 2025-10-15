Remaining backend tasks (high-level)

This is a short, actionable list of remaining features, areas to harden, and recommended next steps.

1. Production-ready embeddings and recommendations
   - Move embeddings to a vector store (pgvector, Pinecone, Milvus) for scalable nearest-neighbor search.
   - Add background jobs to compute embeddings on book create/update.
   - Add caching and API rate-limits for recommendation endpoints.

2. Semantic search
   - Create indexing pipeline (text extraction from e-books, chunking, embeddings) and a search API.
   - Integrate a vector DB or use Postgres+pgvector.

3. Chatbot
   - Build a conversational API with context windowing over the catalog and user history.
   - Add moderation, rate-limiting, and cost controls.

4. Payments & Stripe
   - Add DB migrations for unique constraints and indexes.
   - Harden webhook processing with replay detection, signature rotation, and monitoring.
   - Support refunds and reconciliation reports.

5. Security & Ops
   - Integrate centralized logging, request tracing, and health checks.
   - Add CI/CD, deploy manifests, and DB backups.

6. UX / Frontend
   - Small frontend to browse recommendations and chat with the assistant.

Estimate: medium effort (several sprints) to productionize fully; many features implemented lightly in the codebase already.
