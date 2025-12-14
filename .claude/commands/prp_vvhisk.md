---
description: Project Research Protocol (PRP) Generator for maintenance tasks
allowed-tools: Read, Glob, Grep, WebSearch, mcp__context7__resolve-library-id, mcp__context7__get-library-docs, Write, TodoWrite, LS, Bash
---

# Project Research Protocol (PRP) Generator

You are tasked with creating a comprehensive Project Research Protocol (PRP) for a maintenance task. This protocol will help an AI agent understand the project context and execute the task successfully.

## Input Parameters
- **MAINTENANCE_TASK**: The specific maintenance task to be performed (e.g., "Update React to v18", "Add GitHub Actions CI/CD", "Implement ESLint configuration")

## Research Protocol Structure

### Phase 1: Codebase Analysis
Conduct a thorough analysis of the project structure and current state:

#### 1.1 Project Discovery
- Use `LS` tool to explore the root directory structure
- Identify project type (React, Node.js, Python, etc.)
- Locate configuration files (package.json, pyproject.toml, etc.)
- Document project architecture and key directories

#### 1.2 Package Management Analysis
- Use `Read` tool to examine package.json, requirements.txt, or equivalent
- Identify current versions of relevant packages
- Map dependencies related to the maintenance task
- Check for lock files (package-lock.json, yarn.lock, etc.)

#### 1.3 Build System & Scripts
- Analyze build configuration (webpack, vite, etc.)
- Document available npm scripts or build commands
- Identify testing frameworks and commands
- Check for linting and formatting tools

#### 1.4 CI/CD Pipeline Discovery
- Use `Glob` tool to search for .github/workflows/, .gitlab-ci.yml, etc.
- Analyze existing pipeline configurations
- Document current automation setup

### Phase 2: Documentation Research

#### 2.1 Context7 MCP Research
- Use `mcp__context7__resolve-library-id` to find relevant libraries
- Use `mcp__context7__get-library-docs` to gather up-to-date documentation
- Focus on migration guides, breaking changes, and best practices
- Research compatibility requirements and version constraints

#### 2.2 Web Research
- Use `WebSearch` tool to find:
  - Official migration guides
  - Community best practices
  - Known issues and solutions
  - Performance considerations
  - Security implications

### Phase 3: Task Blueprint Creation

#### 3.1 Implementation Plan
Create a detailed step-by-step plan with:
- Pseudo code for each major step
- Real file references from the codebase
- Command sequences to execute
- Configuration changes needed

#### 3.2 Risk Assessment
- Identify potential breaking changes
- List rollback strategies
- Document testing requirements
- Note compatibility concerns

#### 3.3 Validation Strategy
- Define success criteria
- Create testing checklist
- Specify verification commands
- Plan regression testing

## Output Format

Create a file at `.vvhisper/tasks/{TASK_SUMMARY}.vvhisk.md` with the following structure:

```markdown
# PRP: {MAINTENANCE_TASK}

## Executive Summary
Brief description of the maintenance task and its impact.

## Current State Analysis
### Project Structure
- Project Type: {detected_type}
- Key Directories: {list}
- Configuration Files: {list}

### Dependencies
- Current Version: {current_version}
- Target Version: {target_version}
- Breaking Changes: {list}

### Build System
- Build Tool: {tool_name}
- Scripts: {relevant_scripts}
- Testing: {test_framework}

## Research Findings
### Official Documentation
{context7_findings}

### Community Resources
{web_research_findings}

### Migration Path
{step_by_step_approach}

## Implementation Blueprint
### Prerequisites
- [ ] {prerequisite_1}
- [ ] {prerequisite_2}

### Step-by-Step Implementation
1. **{Step_Title}**
   ```bash
   # Commands to execute
   ```
   - Files to modify: {file_references}
   - Expected outcome: {description}

2. **{Step_Title}**
   ```javascript
   // Pseudo code example
   ```
   - Configuration changes in {specific_file}
   - Testing command: {test_command}

### Validation Checklist
- [ ] {validation_step_1}
- [ ] {validation_step_2}
- [ ] Run tests: `{test_command}`
- [ ] Build succeeds: `{build_command}`
- [ ] No runtime errors in development

## Risk Assessment
### High Risk
- {risk_description} → Mitigation: {strategy}

### Medium Risk
- {risk_description} → Mitigation: {strategy}

### Rollback Plan
1. {rollback_step_1}
2. {rollback_step_2}

## Quality Assessment
### Context Completeness Score: {1-10}/10
- Project analysis: {score}/10
- Documentation coverage: {score}/10
- Implementation detail: {score}/10

### Confidence Score: {1-10}/10
**Rationale:** {detailed_explanation_of_confidence_level}

### Missing Information
- {item_1}
- {item_2}

## Next Steps
1. {immediate_action}
2. {follow_up_action}
```

## Execution Instructions

1. **Initialize Research**: Begin with Phase 1 codebase analysis
2. **Gather Documentation**: Execute Phase 2 research comprehensively
3. **Create Blueprint**: Develop detailed Phase 3 implementation plan
4. **Quality Check**: Ensure all sections are complete and accurate
5. **Output File**: Create the .vvhisk.md file in the correct location

## Quality Checklist
- [ ] All necessary context is included
- [ ] Real file references are provided
- [ ] Commands are project-specific
- [ ] Rollback strategy is defined
- [ ] Success criteria are measurable
- [ ] Confidence score is justified with rationale

## Success Metrics
The PRP should enable an AI agent to:
- Understand the project context completely
- Execute the maintenance task without additional research
- Handle edge cases and potential issues
- Validate the implementation thoroughly
- Rollback if necessary

**Target Confidence Score: 8+/10 for one-pass implementation success**
