---
description: Execute a comprehensive AI-guided maintenance workflow for software projects using a PRP (Project requirement prompt) file
allowed-tools: Read, Glob, Grep, Task, TodoWrite, Bash, Edit, MultiEdit, Write, WebSearch, WebFetch, mcp__context7__resolve-library-id, mcp__context7__get-library-docs
---

# VVhisk: AI-Guided Software Maintenance Workflow

You are about to execute a comprehensive AI-guided maintenance workflow for a software project. The path to the PRP file is: `$ARGUMENTS`

## PHASE 1: READ AND UNDERSTAND THE PRP

**Step 1: Read the PRP File**
- Read the PRP file at the specified path: `$ARGUMENTS`
- This file contains all the context needed for an AI agent to one-pass a maintenance task
- The PRP follows a comprehensive format with Executive Summary, Current State Analysis, Research Findings, Implementation Blueprint, Validation Checklist, Risk Assessment, and Quality Assessment

**Step 2: Understand Context and Requirements**
- Analyze the executive summary to understand the maintenance task scope
- Review the current state analysis to understand the project structure
- Study the implementation blueprint for step-by-step guidance
- Identify all validation requirements and success criteria
- Note any language-specific requirements and dependencies

## PHASE 2: RESEARCH AND CONTEXT GATHERING

**Step 3: Extend Research (If Needed)**
- If the PRP indicates incomplete research, gather additional context:
  - Use Context7 MCP to get up-to-date library documentation
  - Perform web research for best practices and recent updates
  - Analyze the codebase structure using Glob and Grep tools
  - Read relevant configuration files and project documentation

**Step 4: Codebase Analysis**
- Use agents/subagents to perform parallel analysis of:
  - Project structure and architecture patterns
  - Existing implementations that should be followed
  - Current dependencies and their versions
  - Build system and testing infrastructure
  - Code style and conventions

## PHASE 3: ULTRATHINK - COMPREHENSIVE PLANNING

**Step 5: Create Master Plan**
- Use TodoWrite to create a comprehensive task breakdown
- Include all items from the PRP's Implementation Blueprint
- Add any additional tasks discovered during research
- Break down complex tasks into smaller, manageable subtasks
- Assign priorities and identify dependencies between tasks

**Step 6: Agent Task Planning**
- For complex tasks, plan to use subagents with:
  - Extremely clear task descriptions
  - Full context from the PRP
  - Specific instructions to reference the PRP
  - Real context and constraints (never guess imports, file names, etc.)

**Step 7: Implementation Pattern Analysis**
- Identify implementation patterns from existing code
- Determine naming conventions and coding standards
- Locate utility functions and libraries already in use
- Understand the project's testing and validation approaches

## PHASE 4: EXECUTE THE PLAN

**Step 8: Sequential Implementation**
- Execute each task in the TodoWrite plan systematically
- Mark tasks as in_progress before starting, completed when finished
- Use agents/subagents for complex implementations
- Follow the PRP's step-by-step implementation guide
- Ensure each implementation follows existing patterns

**Step 9: Context-Aware Development**
- Always reference the PRP when making decisions
- Follow the project's existing conventions and patterns
- Use real file names, function names, and imports from the codebase
- Implement code that integrates seamlessly with existing systems

## PHASE 5: VALIDATE IMPLEMENTATION

**Step 10: Execute Validation Commands**
- Run each validation command specified in the PRP
- Execute build processes, tests, and linting as required
- Address any failures immediately and re-run until all pass
- Ensure all validation checklist items are satisfied

**Step 11: Quality Assurance**
- Review implementation against PRP requirements
- Verify all acceptance criteria are met
- Check that the implementation follows the project's quality standards
- Ensure proper error handling and edge case coverage

## PHASE 6: COMPLETE AND FINALIZE

**Step 12: Final Validation Suite**
- Run the complete validation suite one final time
- Ensure all tests pass and builds succeed
- Verify that the implementation meets all requirements
- Document any deviations or additional considerations

**Step 13: Completion Report**
- Mark all TodoWrite tasks as completed
- Report completion status with summary of work done
- Re-read the PRP to confirm all requirements are implemented
- Provide final assessment of implementation quality

## CRITICAL EXECUTION PRINCIPLES

1. **Always Reference the PRP**: Continuously refer back to the PRP throughout the process
2. **Never Guess**: Always gather real context about imports, file names, function names, etc.
3. **Use Agents Effectively**: Provide subagents with clear tasks and full PRP context
4. **Follow Existing Patterns**: Adapt to the project's existing code style and architecture
5. **Validate Continuously**: Test and validate at each step, not just at the end
6. **Quality Over Speed**: Ensure correctness and maintainability
7. **Document Decisions**: Keep track of implementation choices and rationale

## ERROR HANDLING AND RECOVERY

- If validation fails, use error patterns from the PRP to fix and retry
- If research reveals missing context, pause to gather necessary information
- If implementation deviates from the PRP, document reasons and get validation
- If subagents fail, provide clearer instructions and more context

## SUCCESS CRITERIA

The workflow is complete when:
- All PRP requirements are implemented
- All validation commands pass
- All TodoWrite tasks are marked complete
- The implementation integrates seamlessly with the existing codebase
- Quality assessment criteria are met

---

**Begin execution by reading the PRP file and creating your comprehensive plan.**
