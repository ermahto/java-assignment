# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
We have two approaches here:

1) OpenAPI-based (Warehouse API)
2) Direct coding (Product, Store)

OpenAPI-first approach:

Pros:
- Clear contract between client and server, no confusion.
- Easy for frontend/other teams to integrate using same spec.
- Auto-generation reduces manual DTO and endpoint mistakes.
- Good tooling support (client SDK, docs, mocks).
- Changes are visible via YAML diff, so easy to review.

Cons:
- Initial setup is little heavy (code generation, config, etc.).
- Any small change requires regen + rebuild.
- Less flexible if API is dynamic or still evolving fast.
- Developers need to understand OpenAPI properly.

Hand-coded approach:

Pros:
- Faster to develop, especially for small APIs.
- Full flexibility, no restriction from spec.
- Easy to modify quickly without regen overhead.
- Good for internal or simple CRUD APIs.

Cons:
- No strict contract, so mismatch risk between client & server.
- More boilerplate (DTO, mapping, validation).
- Documentation may go out of sync.
- Harder to maintain consistency across APIs.

My choice:

If API is public, used by multiple teams, or long-term → I will go with OpenAPI-first approach.

If API is small, internal, or quick prototype → direct coding is fine.

In real projects, I prefer starting with OpenAPI for important modules because it avoids integration issues later and gives better scalability.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
When time is limited, I focus on high-impact tests first.

Priority:
1) Unit tests – cover core business logic, fast and high ROI.
2) Concurrency tests – handle multi-thread issues like lost updates.
3) DB/repository tests – validate queries and ORM behavior.
4) API tests – check endpoints, status codes, basic flows

For maintaining coverage:
- Keep JaCoCo threshold for core modules.
- Use parameterized tests for multiple scenarios.
- Add regression tests for bugs.
- update tests with API changes.

Overall: focus on unit + critical paths, keep integration tests minimal but useful.
```
