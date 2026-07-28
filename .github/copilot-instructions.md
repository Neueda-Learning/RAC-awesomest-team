# Copilot Code Generation Guidelines

## Git Workflow
1. **No direct commits to `main`** - always work in feature branches (`feature/...`, `bugfix/...`, etc.)
2. **Commit message format**:
   - Format: `<type>: <short description>`
   - Types: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`
   - Example: `feat: add configurable transaction data generation`
   - Maximum 72 characters for subject line
3. **Keep related changes in one commit** - group logically related file modifications

## Java/Spring Code Standards
When writing functions, always:
- Add descriptive JSDoc comments
  ```java
  /**
   * Brief description of what the method does.
   * 
   * @param paramName description of the parameter
   * @return description of return value
   * @throws IllegalArgumentException if validation fails with reason
   */
  ```
- Include input validation
  ```java
  if (count <= 0) {
	  throw new IllegalArgumentException("count must be greater than 0");
  }
  ```
- Use early returns for error conditions
- Add meaningful variable names
- Use dependency injection (`@Autowired` or constructor injection)
- Prefer immutability for DTOs and entity fields when applicable

## Spring Boot Service Layer
- Mark services with `@Service`
- Inject repositories via constructor (not field injection)
- Keep business logic in services, not controllers
- Handle exceptions appropriately (let controllers map to HTTP responses)
- Return Optional or List from repository queries

## Controller Layer
- Use `@RestController` and proper `@RequestMapping`
- Validate request bodies with `@Valid`
- Return appropriate HTTP status codes (201 for created, 204 for no content)
- Use `@RequestParam` for query parameters, `@PathVariable` for path segments
- Accept request parameters as DTO objects when there are multiple optional parameters

## Unit Testing (JUnit 5 + Mockito)
- Every service class must have corresponding unit tests
- Use `@ExtendWith(MockitoExtension.class)` for test classes
- Mock external dependencies with `@Mock`
- Test both happy path and error conditions
- Use descriptive test method names: `testMethodName_shouldExpectResult_whenCondition()`
- Verify mock invocations when testing side effects
- Use `ArgumentCaptor` to inspect arguments passed to mocks

## Parameter Validation (especially for data generation)
- Validate numeric ranges (min/max, positive values)
- Validate date/time logic (startAt should not be after endAt)
- Validate parameter combinations (if parameters conflict, throw clear error)
- Provide clear error messages indicating what went wrong

## Data Handling
- Use `LocalDateTime` for timestamps, not `Long` or `Date`
- Set precision for `BigDecimal` amounts: `.setScale(2, RoundingMode.HALF_UP)`
- Document time windows and calculations in comments
- When generating test data, ensure values are realistic and distributed (not all identical)

## Database & Entity Layer
- Use Spring Data JDBC annotations: `@Id`, `@Table`, `@Column` if needed
- Keep entity classes simple - no business logic
- Use constructors for entity initialization
- Add repository query methods with clear names or `@Query` annotations
- Always handle nullable fields explicitly

## Documentation
- Update README.md when adding significant features
- Keep internal documentation files in `/documents`
- Add code comments for non-obvious logic (especially complex queries or rules)
- Document API endpoint behavior in controllers or separate API docs

## File Organization
- Keep generated/compiled output out of Git (`.gitignore` enforced)
- Use consistent package naming: `com.example.monitoring.{module}.{layer}`
- Separate concerns: controllers, services, repositories, entities, DTOs

## Error Handling
- Use custom exceptions or well-known Spring Boot exceptions
- Log errors appropriately
- Let `@RestControllerAdvice` handle exception-to-HTTP-response mapping
- Provide meaningful error messages to API consumers


