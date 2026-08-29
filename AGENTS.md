# Nanpo Window Agent Workflow

## Architecture boundaries

- `frontend/` is the React + Vite application. During feature development it runs independently on port `3000`.
- `backend/` is the Java 21 + Spring Boot application. APIs live under `/api`; during backend development it runs on port `8080`.
- `backend/src/main/resources/static/` is generated output owned by the frontend build. Never edit or commit files in this directory by hand.
- The integrated deliverable is one executable Spring Boot JAR. It serves both `/api/**` and the compiled frontend from port `8080`.

## Required working sequence

1. When `.codegraph/` exists, use `codegraph explore` before grep/find or broad source reads to locate code and understand call paths.
2. Keep frontend and backend changes inside their own directories. Define cross-boundary contracts as `/api` endpoints; the Vite development server proxies `/api` to `http://localhost:8080`.
3. For frontend-only development, run `npm --prefix frontend run dev`.
4. For backend-only development, run `./mvnw -pl backend spring-boot:run -Dskip.frontend=true` with Java 21.
5. Before every final commit, run `./scripts/verify.sh`. This performs a clean Maven build, installs Node locally through Maven, runs `npm ci`, compiles the frontend into the backend static directory, runs backend tests, synchronizes CodeGraph, and checks the diff.
6. Do not commit when the integrated build or tests fail. Do not bypass the frontend build with `-Dskip.frontend=true` during final verification.
7. Review `git status` after verification. Generated frontend assets, `node_modules`, Maven `target` directories, IDE files, and the local CodeGraph database must remain untracked.
8. Commit source and workflow changes together, then push only after the user has authorized the repository update.

## Dependency and packaging rules

- Preserve `frontend/package-lock.json`; use `npm ci` in reproducible builds.
- Node downloads and npm packages default to npmmirror through Maven properties. Both endpoints must remain overridable with `-Dnode.download.root=...` and `-Dnpm.registry=...`.
- Use the root Maven wrapper for integrated builds. Do not require a globally installed Node.js or Maven in CI/Docker.
- Docker builds must run the same root Maven lifecycle and expose only backend port `8080`.
