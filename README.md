# 🎙️ Voice Agent

> **Audio in, context-aware AI voice out.** A Spring Boot backend that transcribes speech, enriches it with retrieval-augmented context (RAG), generates an intelligent reply via a local Ollama model (Gemma 3:4b), and speaks it back — all in one HTTP request.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Deepgram](https://img.shields.io/badge/Deepgram-STT%20%2B%20TTS-blue)
![Ollama](https://img.shields.io/badge/Ollama-Gemma%203%3A4b-purple)
![RAG](https://img.shields.io/badge/RAG-Enabled-red)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**Author:** Sohail Ahmad

---

## Table of Contents

1. [What This Project Does](#what-this-project-does)
2. [The Full Pipeline at a Glance](#the-full-pipeline-at-a-glance)
3. [Step-by-Step Walkthrough](#step-by-step-walkthrough)
4. [Architecture](#architecture)
5. [Component Responsibilities](#component-responsibilities)
6. [RAG — Context-Aware Answers](#rag--context-aware-answers)
7. [Data Flow in Detail](#data-flow-in-detail)
8. [Tech Stack](#tech-stack)
9. [Prerequisites](#prerequisites)
10. [Configuration](#configuration)
11. [Running the Application](#running-the-application)
12. [API Reference](#api-reference)
13. [Testing the Pipeline](#testing-the-pipeline)
14. [Project Structure](#project-structure)
15. [Key Design Decisions](#key-design-decisions)
16. [Development Milestones](#development-milestones)
17. [Current Scope vs. Future Work](#current-scope-vs-future-work)
18. [License](#license)

---

## What This Project Does

**Voice Agent** is a Spring Boot backend that implements a complete, **synchronous, context-aware voice-agent pipeline**:

```
Audio → Speech-to-Text → RAG Retrieval → LLM → Text-to-Speech → Audio Response
```

A user uploads an audio file. The backend:

1. **Transcribes** it into text using **Deepgram Speech-to-Text**
2. **Retrieves relevant context** from a knowledge base using **RAG (Retrieval-Augmented Generation)**
3. **Generates a grounded reply** using a **locally-running Ollama model (Gemma 3:4b)** — the LLM is prompted with the retrieved context, so answers are **context-aware** rather than hallucinated
4. **Synthesizes that reply into speech** using **Deepgram Text-to-Speech**
5. **Returns the generated MP3** directly in the HTTP response

The LLM runs entirely on your machine via Ollama — no cloud LLM provider, no inference cost, and no prompt/audio data leaving your machine to a third-party AI service (only STT and TTS touch Deepgram).

The **RAG layer** turns a plain question-answering bot into a knowledgeable assistant: instead of the LLM answering from its general training alone, it answers **based on your specific documents / knowledge base**, reducing hallucinations and enabling domain-specific responses.

---

## The Full Pipeline at a Glance

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Postman / cURL)                         │
│                    multipart/form-data — "file"                        │
└────────────────────────────────┬───────────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       VoiceAgentController                             │
│             (HTTP boundary — accepts audio, returns MP3)               │
└────────────────────────────────┬───────────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       AudioFileValidator                               │
│            (Empty check · Content type · Size ≤ 15 MB)                 │
└────────────────────────────────┬───────────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       VoiceAgentService                                │
│                 (Orchestration — coordinates the stages)               │
└──┬──────────────┬─────────────────────┬────────────────────┬───────────┘
   │              │                     │                    │
   ▼              ▼                     ▼                    ▼
┌────────┐   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Deepgram│   │ RagService   │    │OllamaLlm     │    │DeepgramTts   │
│Service │   │ (Retriever)  │    │Service       │    │Service       │
│(STT)   │   │              │    │(Gemma 3:4b)  │    │(TTS)         │
└───┬────┘   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
    │               │                   │                   │
    ▼               ▼                   ▼                   ▼
Transcript     Relevant           Context-Aware         Audio Bytes
               Context            Reply Text
    │               │                   │                   │
    └───────────────┴───────────────────┴───────────────────┘
                                 │
                                 ▼
                      audio/mpeg Response
```

**In plain English:**

- You **speak** → your voice becomes a file → the file is sent to the server
- The server asks Deepgram: *"What did they say?"* → gets a transcript
- The server asks RAG: *"What in my knowledge base is relevant to this question?"* → gets relevant context
- The server asks Gemma: *"Using this context, how should we reply?"* → gets a **grounded** answer
- The server asks Deepgram: *"Say this out loud"* → gets an MP3
- The server sends that MP3 back to you

---

## Step-by-Step Walkthrough

Here is exactly what happens when a client calls `POST /api/voice/process` with an audio file.

### Step 1 — Request arrives at the controller

`VoiceAgentController` receives a `multipart/form-data` request with a single field named `file`, wrapped in a `MultipartFile`. The controller does **not** trust it yet.

### Step 2 — Audio file validation

`AudioFileValidator` runs before any external call:

- Is the file **empty**? → reject with `400 Bad Request`
- Is the **content type** a supported audio format? → reject if not
- Is the file **larger than 15 MB**? → reject

This prevents wasting Deepgram API quota on garbage input and protects the server from oversized uploads.

### Step 3 — Audio → Transcript (Speech-to-Text)

The audio bytes are passed to `DeepgramService.transcribe(...)`, which:

1. Makes a `POST` request to `https://api.deepgram.com/v1/listen`
2. Sends the raw audio bytes as the body with the correct `Content-Type`
3. Requests `smart_format=true` (adds punctuation/casing) using the configured model
4. Receives a **deeply nested JSON** response from Deepgram

Deepgram's response looks roughly like this:

```json
{
  "metadata": { "request_id": "...", "duration": 2.73 },
  "results": {
    "channels": [
      {
        "alternatives": [
          { "transcript": "What is our refund policy?" }
        ]
      }
    ]
  }
}
```

`DeepgramService` **extracts only the transcript string** and returns a clean `TranscriptionResponse(transcript)`. The rest of the JSON is discarded. This keeps the rest of the application decoupled from Deepgram's schema.

### Step 4 — Transcript sanity check

Back inside `VoiceAgentService`, if the transcript is `null` or blank (e.g., silence or noise), the pipeline is aborted with a clear error. There is no point asking the LLM to respond to nothing.

### Step 5 — Retrieve relevant context (RAG)

The transcript is used as a **query** to the RAG layer:

1. `RagService` **embeds** the transcript into a vector using an embedding model
2. It searches the **vector store** for the top-K most similar chunks of indexed documents
3. It returns the retrieved **context passages** as plain text

This is what turns the LLM's reply from *"generic"* into *"grounded in your actual knowledge base."* If no relevant context is found, the pipeline gracefully falls back to a plain LLM response (or `RagService` returns an empty context string).

### Step 6 — Transcript + Context → Response Text (LLM)

`OllamaLlmService.generateResponse(transcript, context)` is called. Internally, it builds a prompt like:

```
You are a helpful assistant. Answer the user's question using ONLY the
context below. If the answer is not in the context, say you don't know.

Context:
<retrieved chunks from RAG>

Question:
<transcript>
```

This prompt is sent to the local Ollama endpoint (`http://localhost:11434/api/generate`) with the configured model (`gemma3:4b`). The response is parsed into an `LlmResponse` object.

> ⚠️ **Critical detail:** the code calls `llmResponse.response()` to get the **actual text**. It must **not** call `llmResponse.toString()`, which returns the object's debug representation (`LlmResponse[response=...]`) — that would then be synthesized as literal text by TTS. This was a real bug that was fixed.

### Step 7 — Response Text sanity check

If the LLM returns an empty response, the pipeline aborts. Passing empty text to TTS would produce an empty or garbage audio file.

### Step 8 — Response Text → Speech (Text-to-Speech)

The generated text is passed to `DeepgramTtsService.synthesize(responseText)`, which:

1. Makes a `POST` request to `https://api.deepgram.com/v1/speak`
2. Sends the text with the configured voice and encoding
3. **Receives raw audio bytes** (`byte[]`) from Deepgram

The service returns these bytes directly.

### Step 9 — Audio is returned to the client

Back in the controller, the audio bytes are wrapped in a `ResponseEntity<byte[]>` with:

- `Content-Type: audio/mpeg`
- `Content-Disposition: attachment; filename="voice-response.mp3"`
- Correct `Content-Length`

The client saves the response as an `.mp3` file and plays it.

---

## Architecture

The project follows a strict **layered architecture**:

```
Controller  →  Application Service  →  External Clients
```

Each layer has **one job**, and nothing leaks across boundaries.

```
                        ┌──────────────────────────┐
                        │        Client            │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │  VoiceAgentController    │   ← thin HTTP layer
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │  VoiceAgentService       │   ← orchestration
                        └──┬──────────┬──────────┬─┘
                           │          │          │
                ┌──────────▼──┐  ┌────▼─────┐  ┌─▼──────────┐
                │ Deepgram    │  │ RagService│ │ Deepgram   │
                │ Service STT │  │ + Vector  │ │ TTS Service│
                │             │  │ Store     │ │            │
                └─────────────┘  └────┬──────┘ └────────────┘
                                      │
                                ┌─────▼─────────┐
                                │ Ollama LLM    │
                                │ (Gemma 3:4b)  │
                                └───────────────┘
```

### Layer responsibilities

| Layer | Responsibility | Must **not** do |
|---|---|---|
| **Controller** | Accept request, delegate validation, call service, format response | Contain AI/API logic |
| **Orchestration Service** | Coordinate STT → RAG → LLM → TTS, guard against empty results | Implement raw HTTP calls |
| **External Services** | Encapsulate Deepgram, Ollama, RAG/vector-store calls | Know about the other stages |
| **DTOs** | Type-safe representation of external payloads | Contain business logic |
| **Properties** | Typed configuration binding | Be hardcoded in Java |

---

## Component Responsibilities

| Class | Responsibility |
|---|---|
| **`VoiceAgentController`** | HTTP boundary. Accepts the file, delegates validation, calls `VoiceAgentService.process(...)`, returns the MP3. ~20 lines, intentionally trivial. |
| **`AudioFileValidator`** | Source of truth for audio validation — empty check, content-type check, 15 MB max size. Uses `DataSize` correctly. |
| **`VoiceAgentService`** | **Orchestrator.** Calls STT → RAG → LLM → TTS in order, guards against empty intermediate results, returns final `byte[]`. The only class that knows the pipeline order. |
| **`DeepgramService`** | Owns the STT API contract. Sends audio bytes, parses Deepgram's nested JSON, extracts the transcript into a domain-level `TranscriptionResponse`. |
| **`RagService`** | Owns the **RAG layer**. Embeds the query, retrieves top-K relevant chunks from the vector store, and returns a context string. |
| **`OllamaLlmService`** | Owns the Ollama API contract. Builds the prompt (transcript + RAG context), sends to Ollama, parses the response into an `LlmResponse`. |
| **`DeepgramTtsService`** | Owns the TTS API contract. Sends text, receives audio bytes, returns them untouched. |
| **DTOs** (`DeepgramResponse`, `TranscriptionResponse`, `LlmRequest`, `LlmResponse`) | Mirror external payload shapes so parsing is explicit and typed. `TranscriptionResponse` is domain-level, not Deepgram's raw JSON. |
| **`DeepgramProperties`, `OllamaProperties`, `RagProperties`** | Bind externalized configuration (API keys, URLs, model names, top-K, similarity thresholds). |

---

## RAG — Context-Aware Answers

**RAG (Retrieval-Augmented Generation)** is what makes the agent's answers **grounded in a specific knowledge base** rather than relying purely on the LLM's training data.

### Why RAG?

Without RAG, `gemma3:4b` answers from general knowledge only. With RAG, the model is given **relevant documents** at prompt time, so it can:

- **Answer domain-specific questions** (policies, docs, product manuals, internal wikis)
- **Reduce hallucinations** — the model is instructed to answer *only* from the retrieved context
- **Stay current** — you update the knowledge base, not the model
- **Cite sources** (optional) — retrieved chunk IDs can be surfaced alongside the answer

### How the RAG step works

```
Transcript ("What is our refund policy?")
        │
        ▼
┌───────────────────────┐
│  1. Embed the query   │  ← embedding model
└──────────┬────────────┘
           │  query vector
           ▼
┌───────────────────────┐
│  2. Vector search     │  ← top-K nearest neighbors
│     (cosine sim)      │
└──────────┬────────────┘
           │  top-K chunks
           ▼
┌───────────────────────┐
│  3. Build context     │  ← concatenate chunks
│     string            │
└──────────┬────────────┘
           │  "context" text
           ▼
   OllamaLlmService
   (transcript + context → prompt)
```

### Knowledge base

- Documents are **ingested once** (chunked + embedded + indexed in a vector store)
- At query time, only the **relevant top-K chunks** are retrieved
- If the vector store is empty or no chunks meet the similarity threshold, the RAG step returns empty context and the LLM answers without augmentation

### Tuning knobs

| Parameter | Purpose |
|---|---|
| `rag.top-k` | How many chunks to retrieve |
| `rag.similarity-threshold` | Minimum similarity score to include a chunk |
| `rag.embedding-model` | Model used to embed queries and documents |
| `rag.vector-store.url` | Vector store connection (if external) |

> 📝 **Note:** update class names (`RagService`, `DocumentRetriever`, `VectorStore`) and parameter names to match your actual implementation.

---

## Data Flow in Detail

```
MultipartFile file
      │  (validated by AudioFileValidator)
      ▼
byte[] audioBytes  +  String contentType
      │
      ▼
DeepgramService.transcribe(byte[], String)
      │
      ▼
TranscriptionResponse { transcript }
      │
      ▼
String transcript
      │
      ├──────────────────────────────┐
      │                              │
      ▼                              ▼
RagService.retrieve(transcript)   (transcript)
      │                              │
      ▼                              │
String context                       │
      │                              │
      └──────────────┬───────────────┘
                     ▼
OllamaLlmService.generateResponse(transcript, context)
                     │
                     ▼
LlmResponse { response }
                     │
                     ▼
String responseText
                     │
                     ▼
DeepgramTtsService.synthesize(String)
                     │
                     ▼
byte[] audioResponse
                     │
                     ▼
ResponseEntity<byte[]>  (Content-Type: audio/mpeg)
```

Each arrow is a **clean type transition**. No stage leaks its external schema into the next.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3+ |
| Build Tool | Maven |
| HTTP Client | Spring `RestClient` |
| Speech-to-Text | Deepgram |
| RAG | Embedding model + vector store |
| LLM | Ollama — Gemma 3:4b (local) |
| Text-to-Speech | Deepgram |
| Testing | Postman / cURL |

---

## Prerequisites

Before running the application:

- **Java 17+**
- **Maven**
- **Ollama** installed and running locally
- **Gemma 3:4b** model pulled
- A **Deepgram API key**
- **Vector store** for RAG (in-memory, pgvector, Chroma, Qdrant, or similar)

### Install Ollama and pull the model

```bash
# Install Ollama (see https://ollama.com for your OS)
ollama pull gemma3:4b
ollama serve
```

Ollama will listen at `http://localhost:11434` by default.

---

## Configuration

All configuration is externalized. **Never hardcode API keys in source code.**

In `src/main/resources/application.properties` (or `.yml`):

```properties
# Deepgram
deepgram.api-key=${DEEPGRAM_API_KEY}
deepgram.base-url=https://api.deepgram.com
deepgram.model=nova-3
deepgram.smart-format=true

# Ollama
ollama.base-url=http://localhost:11434
ollama.model=gemma3:4b

# Audio
audio.max-file-size=15MB

# RAG
rag.top-k=4
rag.similarity-threshold=0.75
rag.embedding-model=<your-embedding-model>
rag.vector-store.url=<your-vector-store-url>
```

Export your Deepgram key:

```bash
export DEEPGRAM_API_KEY=your_api_key_here
```

These values are bound to typed `@ConfigurationProperties` classes (`DeepgramProperties`, `OllamaProperties`, `RagProperties`), so any service that needs a config value receives it via constructor injection — never via magic constants.

---

## Running the Application

```bash
# Clone
git clone https://github.com/sohailahmad/voice-agent.git
cd voice-agent

# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

---

## API Reference

### `POST /api/voice/process`

The main (and currently only production-intent) endpoint.

**Request**

```
POST http://localhost:8080/api/voice/process
Content-Type: multipart/form-data

field: file = <audio file>
```

- **Field name:** `file` (required)
- **Supported audio:** whatever Deepgram accepts — e.g. `audio/wav`, `audio/mpeg`, `audio/webm`, `audio/ogg`, `audio/flac`
- **Max size:** 15 MB

**Response**

```
HTTP/1.1 200 OK
Content-Type: audio/mpeg
Content-Disposition: attachment; filename="voice-response.mp3"
Content-Length: <bytes>
```

The body is an MP3 containing the context-aware synthesized reply.

**cURL example**

```bash
curl -X POST http://localhost:8080/api/voice/process \
  -F "file=@sample.wav" \
  --output voice-response.mp3
```

**Postman example**

1. Method: `POST`
2. URL: `http://localhost:8080/api/voice/process`
3. Body → **form-data**
4. Key: `file` (type: **File**) → choose your audio file
5. Send → click **"Save Response" → "Save to a file"** → name it `voice-response.mp3`

> ⚠️ **Postman note:** Postman may not render audio inline. It might show the response as empty or as an attachment placeholder. **This is not a failure.** Save the response body to a file and play it locally.

**Error cases**

| Condition | HTTP status |
|---|---|
| File missing / empty | `400 Bad Request` |
| Unsupported content type | `400 Bad Request` |
| File > 15 MB | `400` / `413` |
| No speech detected in audio | `400` |
| RAG returns empty context (graceful fallback) | *(continues to LLM)* |
| LLM returned empty text | `500` |
| Deepgram / Ollama failure | `5xx` |

---

## Testing the Pipeline

The pipeline was **built incrementally** — each stage was validated in isolation before being wired into the full flow. This is the recommended way to debug any future change.

### Milestones that were independently verified

| Test | Pipeline | Status |
|------|----------|--------|
| 1 | Audio → Deepgram → Transcript | ✅ |
| 2 | Prompt → Ollama → Response | ✅ |
| 3 | Text → Deepgram TTS → Audio | ✅ |
| 4 | Audio → Deepgram → Ollama | ✅ |
| 5 | Ollama → Deepgram TTS → Audio | ✅ |
| 6 | **Audio → Deepgram → Gemma → Deepgram TTS → Audio** | ✅ |
| 7 | **Full pipeline + RAG** (context-aware end-to-end) | ✅ |

The intermediate controllers (`TextToSpeechController`, `LlmToSpeechController`, `VoiceConversationController`) exist to support isolation tests. They are still in the codebase — **do not delete them without confirming they're unused.**

### Quick manual smoke test

```bash
# 1. Record a short clip (or use any .wav / .mp3 file)
# 2. Send it
curl -X POST http://localhost:8080/api/voice/process \
  -F "file=@sample.wav" \
  --output reply.mp3

# 3. Play it
open reply.mp3        # macOS
mpg123 reply.mp3      # Linux
start reply.mp3       # Windows
```

You should hear the AI reply — **grounded in your knowledge base**.

---

## Project Structure

```
voice-agent/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/agent/voice_agent/
    │   ├── VoiceAgentApplication.java
    │   │
    │   ├── controller/
    │   │   ├── VoiceAgentController.java        # 🎯 Main endpoint
    │   │   ├── TextToSpeechController.java      # dev/test
    │   │   ├── LlmToSpeechController.java       # dev/test
    │   │   └── VoiceConversationController.java # dev/test
    │   │
    │   ├── service/
    │   │   ├── VoiceAgentService.java           # 🎯 Orchestrator
    │   │   ├── DeepgramService.java             # STT
    │   │   ├── RagService.java                  # RAG retrieval
    │   │   ├── OllamaLlmService.java            # LLM (Gemma 3:4b)
    │   │   ├── DeepgramTtsService.java          # TTS
    │   │   ├── VoiceConversationService.java    # dev/test
    │   │   └── LlmToSpeechService.java          # dev/test
    │   │
    │   ├── component/
    │   │   └── AudioFileValidator.java          # validation
    │   │
    │   ├── dto/
    │   │   ├── DeepgramResponse.java
    │   │   ├── TranscriptionResponse.java
    │   │   ├── LlmRequest.java
    │   │   ├── LlmResponse.java
    │   │   └── ...                              # RAG DTOs (context, chunks)
    │   │
    │   ├── config/
    │   │   ├── DeepgramProperties.java
    │   │   ├── OllamaProperties.java
    │   │   ├── RagProperties.java
    │   │   └── ...
    │   │
    │   └── ...
    │
    └── resources/
        ├── application.properties
        └── application.yml
```

---

## Key Design Decisions

This section explains **why** the code is shaped the way it is. If you're extending the project, respect these decisions unless you have a concrete reason to change them.

### 1. Synchronous pipeline (no streaming yet)
The whole audio → text → context → text → audio flow completes inside a single HTTP request. Streaming would require WebSockets or SSE and a significant redesign. The current baseline prioritizes **correctness and simplicity** first; streaming is a future addition.

### 2. Local LLM instead of a cloud LLM
Ollama runs Gemma 3:4b on the local machine. This means **no cloud LLM bill**, **no prompt data leaving the machine**, and **predictable latency**. The trade-off is that response quality is bounded by the local model, and inference is limited by local hardware.

### 3. RAG is a distinct pipeline stage
`RagService` is its own component — separate from `OllamaLlmService`. This lets you swap the retrieval backend (vector store) or the embedding model independently of the LLM. It also means the RAG step can be **skipped** for queries that don't need context (e.g., small talk) with a simple conditional.

### 4. Extract the transcript — don't leak Deepgram's JSON
`DeepgramService` converts Deepgram's deeply nested JSON into a flat `TranscriptionResponse`. Downstream code never touches `results.channels[0].alternatives[0]...`. This makes it trivial to swap Deepgram for another STT provider later.

### 5. Use `llmResponse.response()`, not `llmResponse.toString()`
A previous bug: calling `.toString()` on the LLM DTO passed `"LlmResponse[response=Hello there!...]"` to TTS, which spoke the class name literally. **Always extract the field.**

### 6. Configuration via `@ConfigurationProperties`
API keys, model names, top-K values, and URLs live in properties files, bound to typed classes. No `@Value` scattered around, no hardcoded constants in Java.

### 7. Constructor injection everywhere
No field injection (`@Autowired` on fields). This makes dependencies explicit, testable, and immutable-by-default.

### 8. Validation lives in `AudioFileValidator` — not in controllers
If you add another audio endpoint later, it reuses the validator. Duplicating the checks inside each controller would guarantee divergence.

### 9. `DataSize` is handled properly
The 15 MB limit is expressed as `DataSize.ofMegabytes(15)`. Code uses the appropriate `DataSize` API — not `.getBytes()` on the wrong type (which was a real bug that was fixed).

### 10. Keep the working baseline intact
`VoiceAgentService`, `DeepgramService`, `RagService`, `OllamaLlmService`, `DeepgramTtsService`, and `AudioFileValidator` are the **baseline**. Future features should **add** to them, not rewrite them.

---

## Development Milestones

The project was built in strict incremental order — one stage at a time, verified before moving on:

1. **Audio → Deepgram → Transcript** — verify STT works
2. **Prompt → Ollama → Response** — verify LLM works
3. **Text → Deepgram TTS → Audio** — verify TTS works
4. **Audio → Deepgram → Ollama** — verify STT + LLM together
5. **Ollama → Deepgram TTS → Audio** — verify LLM + TTS together
6. **Audio → Deepgram → Gemma → Deepgram TTS → Audio** — full pipeline
7. **Add RAG** — context retrieval + grounded generation

Each stage had its own test endpoint (still present) so that failures could be isolated without running the full pipeline.

---

## Current Scope vs. Future Work

### ✅ Implemented and working

- Audio upload + validation
- Deepgram STT (transcript extraction)
- **RAG retrieval for context-aware answers**
- Ollama / Gemma 3:4b grounded response generation
- Deepgram TTS (`audio/mpeg` output)
- Full synchronous pipeline via `POST /api/voice/process`
- Externalized configuration
- Clean layered architecture

### 🔮 Not yet implemented (future roadmap)

- Conversation memory (multi-turn context across requests)
- WebSocket / real-time streaming audio in and out
- Microphone capture in the browser
- Twilio telephony integration (phone calls in/out)
- Authentication, authorization, rate limiting
- Persistence (e.g., MongoDB for call history)
- Frontend UI
- Async / non-blocking responses
- Production deployment (Docker, cloud infra, observability)

Adding any of these should be done **incrementally** on top of the current baseline, with one major feature at a time.

---

## License

This project is licensed under the MIT License — see the `LICENSE` file for details.

---

## Author

**Sohail Ahmad**

- GitHub: [@sohailahmad](https://github.com/sohailahmad)


---

## 🙏 Acknowledgements

- [Deepgram](https://deepgram.com) — Speech-to-Text & Text-to-Speech
- [Ollama](https://ollama.com) — Local LLM runtime
- [Gemma 3](https://ai.google.dev/gemma) — Open model
- [Spring Boot](https://spring.io/projects/spring-boot) — Application framework
