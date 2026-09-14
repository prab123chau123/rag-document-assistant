# RAG Document Assistant

A Spring Boot API that answers questions about uploaded PDF and text documents using local Ollama models and PostgreSQL with pgvector.

## Requirements

- Java 17 or newer
- Ollama running at `http://localhost:11434`
- PostgreSQL with the `vector` extension enabled in the application database

Download the models:

```bash
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

## Configuration

Defaults in `src/main/resources/application.yml` target a local development database:

| Environment variable | Default |
| --- | --- |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `DB_HOST` | `127.0.0.1` |
| `DB_PORT` | `5433` |
| `DB_NAME` | `vectordb` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Supply your database credentials as environment variables. Spring Boot does not automatically load `.env` files. No OpenAI API key is required.

The app creates the `document_chunks_nomic` table. Ollama and PostgreSQL must be available during startup because initialization generates an embedding to determine its vector dimension and initializes the store.

## Run

```bash
./mvnw spring-boot:run
```

The API listens on `http://127.0.0.1:8080`.

## Upload a document

```bash
curl -i http://localhost:8080/api/documents \
  -F 'file=@/absolute/path/document.pdf'
```

Supported files: UTF-8 `.txt` and text-based `.pdf`, up to 5 MB and 200,000 extracted characters. Scanned PDFs require OCR before uploading.

The response contains `documentId`, `filename`, and `chunksStored`.

## Ask a question

Use the `documentId` returned by the upload endpoint:

```bash
curl -i http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"Summarize this document.","documentId":"REPLACE_WITH_UPLOAD_UUID"}'
```

The response contains `answer` and `sources`. A frontend should retain the upload ID and send it automatically with questions.

## How retrieval works

Documents are split into chunks, embedded with `nomic-embed-text`, and stored with their text and metadata in pgvector. Questions are embedded with the same model and searches are filtered by document ID.

Short documents (up to 16 chunks and 16,000 characters) supply all chunks in document order. Larger documents supply the highest-scoring excerpts according to retrieval settings. `llama3.2:3b` generates an answer using those excerpts. Model answers can be inaccurate; source excerpts allow review.

## Tests

Run the retrieval unit tests without external services:

```bash
./mvnw -Dtest=QuestionServiceTest test
```

The full test suite also includes a Spring context startup test and requires Ollama, both models, and PostgreSQL.

This is a local learning project. Uploaded text persists in PostgreSQL. The API has no user authentication or document ownership checks; a document ID is a retrieval filter, not an access-control mechanism.
