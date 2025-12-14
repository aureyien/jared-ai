# Create BASE PRP

## Feature: $ARGUMENTS

Generate a complete PRP for feature implementation with deep and thorough research. The AI agent MUST receive rich context through the PRP to enable one-pass implementation success through self-validation and iterative refinement.

The AI agent only gets the context appended to the PRP and its own training data. The PRP MUST include all research findings and reference materials. The Agent has WebSearch capabilities, therefore you SHALL pass URLs to documentation and examples.

## Research Process

> During the research process, you MUST create clear tasks and SHALL spawn as many agents and subagents as needed using batch tools. You SHALL PRIORITIZE research depth over speed to optimize for implementation success.

### 1. **Codebase Analysis in Depth**
   - You MUST create clear todos and spawn subagents to search the codebase for similar features/patterns
   - You SHALL INFER the best research approach based on the feature requirements
   - You MUST identify all necessary files to reference in the PRP
   - You MUST PRESERVE all existing conventions found in the codebase
   - You SHALL VERIFY existing test patterns for validation approach
   - You MUST use batch tools to spawn subagents for parallel codebase searches

### 2. **External Research at Scale**
   - You MUST create clear todos with detailed instructions for subagents to perform deep research
   - You SHALL include URLs to all relevant documentation and examples found
   - For critical documentation pieces, you MUST:
     - Create .md files in PRPs/ai_docs
     - Reference them in the PRP with clear reasoning and instructions
   - You SHOULD search for:
     - Library documentation (MUST include specific URLs)
     - Implementation examples (GitHub/StackOverflow/blogs)
     - Best practices and common pitfalls
   - You SHALL use batch tools to spawn subagents for parallel web searches

### 3. **User Clarification**
   - You SHOULD CLARIFY any ambiguous requirements before proceeding
   - You MAY ask for additional context if it would improve the PRP quality

### Critical Context Requirements

The PRP MUST include:

- **Documentation**: URLs with specific sections that the AI agent SHALL reference
- **Code Examples**: Real snippets from codebase that the AI MUST follow
- **Gotchas**: Library quirks and version issues the AI MUST NOT ignore
- **Patterns**: Existing approaches the AI SHALL PRESERVE
- **Best Practices**: Common pitfalls the AI SHOULD avoid

### Implementation Blueprint

You MUST:
- Start with pseudocode showing the implementation approach
- Reference real files for pattern examples
- Include comprehensive error handling strategy
- List all tasks in execution order using information-dense keywords

### Validation Gates

The validation gates MUST be executable by the AI agent:

```bash
# Linting
npm run lint

# Unit Tests - REQUIRED
npm run test
```

You SHOULD include multiple validation gates:
- Tests (REQUIRED)
- MCP servers (RECOMMENDED)
- Additional creative validation methods (OPTIONAL)

## Critical Workflow Requirement

**BEFORE writing the PRP, you MUST:**

1. COMPLETE all research and codebase exploration
2. CREATE detailed todos planning your PRP approach
3. VERIFY all gathered information is accurate
4. ONLY THEN begin writing the PRP

## Output

You MUST save the PRP as: `PRPs/{feature-name}.md`

## Quality Checklist

The PRP MUST include:
- [ ] All necessary context for one-pass implementation
- [ ] Executable validation gates
- [ ] References to existing patterns
- [ ] Clear implementation path
- [ ] Documented error handling

You SHALL score the PRP on a scale of 1-10 (confidence level for one-pass implementation success using Claude Code).

**Remember**: The goal is one-pass implementation success through comprehensive context. You MUST PRIORITIZE completeness over brevity.