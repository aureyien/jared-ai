# Execute BASE PRP
  
Implement a feature using the PRP file.

  PRP File: $ARGUMENTS

  Execution Process

  1. Load PRP
    - MUST read the specified PRP file
    - SHALL understand all context and requirements
    - MUST follow all instructions in the PRP and SHOULD extend research if needed
    - SHALL VERIFY all needed context is available to implement the PRP fully
    - MAY perform additional web searches and codebase exploration as required
  2. ULTRATHINK
    - MUST ultrathink before executing the plan
    - SHALL create a comprehensive plan addressing all requirements
    - MUST break down the PRP into clear todos using the TodoWrite tool
    - SHOULD use agents, subagents, and batchtool to enhance the process
    - CRITICAL: MUST ENSURE extremely clear tasks for subagents with proper context references
    - SHALL VERIFY each subagent reads the PRP and understands its context
    - MUST identify implementation patterns from existing code
    - MUST NEVER guess about imports, file names, or function names
    - SHALL ALWAYS base decisions on real context gathering
  3. Execute the Plan
    - SHALL implement all code according to the PRP specifications
    - MUST PRESERVE existing code patterns and conventions
    - SHALL PRIORITIZE accuracy over speed
  4. Validate
    - MUST run each validation command
    - SHALL VERIFY thorough validation for implementation confidence
    - MUST fix any failures IMMEDIATELY
    - SHALL re-run validation until all tests pass
    - MUST ALWAYS re-read the PRP to validate implementation meets requirements
  5. Complete
    - SHALL VERIFY all checklist items are completed
    - MUST run final validation suite
    - SHALL report completion status
    - MUST re-read the PRP to VERIFY complete implementation
  6. Reference the PRP
    - MAY reference the PRP again at any time if CLARIFICATION is needed

  Note: If validation fails, MUST use error patterns in PRP to fix and IMMEDIATELY retry.