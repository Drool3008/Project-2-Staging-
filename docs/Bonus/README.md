# Bonus Task: LLM Benchmarking Setup

## Overview

This bonus task provides a **controlled empirical comparison** of LLM refactoring capabilities using:
- **One prompt** (controlled variable)
- **Two models** (LLM-1 and LLM-2)
- **Two modes** (Non-agentic and Agentic)
- **Same code** (PageServlet God Class)
- **Same constraints** (behavior preservation, test safety)

## Structure

```
Bonus/
├── prompt.md                    # Single fixed prompt for all experiments
├── outputs/
│   ├── llm1_non_agentic.md     # LLM-1 single-shot output
│   ├── llm1_agentic.md         # LLM-1 with multi-step reasoning
│   ├── llm2_non_agentic.md     # LLM-2 single-shot output
│   └── llm2_agentic.md         # LLM-2 with multi-step reasoning
└── Benchmarking_Report.md       # Comparative analysis
```

## How to Complete This Task

### Step 1: Choose Your Models

Replace `LLM-1` and `LLM-2` with actual model names:
- **Option A:** LLM-1 = GPT-4o, LLM-2 = Claude 3.5 Sonnet
- **Option B:** LLM-1 = Claude 3.5 Sonnet, LLM-2 = GPT-4o
- **Other:** Any two frontier LLMs

### Step 2: Run Non-Agentic Experiments

For **both models**:
1. Open the model's web interface (ChatGPT/Claude)
2. Copy the entire content of `prompt.md`
3. Paste and run **once** (no iteration)
4. Copy the complete response
5. Paste into the corresponding `outputs/llm{1,2}_non_agentic.md` file
6. Replace the placeholder content

### Step 3: Run Agentic Experiments

For **both models**:
1. Use the same `prompt.md` content
2. Run through your Task-4 agent loop (or similar agentic framework)
3. Document the full reasoning trace:
   - Step-by-step decisions
   - Tool calls (grep, diff, AST if available)
   - Self-corrections
4. Copy the final refactoring output
5. Paste into the corresponding `outputs/llm{1,2}_agentic.md` file
6. Replace the placeholder content

**Note:** If you don't have agentic tooling for a model, you can simulate by allowing multiple iterations and tool suggestions.

### Step 4: Analyze and Complete the Report

Fill in `Benchmarking_Report.md`:

1. **Section 1:** Update experimental setup with actual model names
2. **Section 2:** Compare smell identification accuracy across 4 outputs
3. **Section 3:** Evaluate refactoring quality (behavior preservation, test safety, over/under engineering)
4. **Section 4:** Document constraint violations and hallucinations
5. **Section 5:** Analyze where agentic mode helped vs hurt
6. **Section 6:** Overall verdict comparing models and modes

### Step 5: Submit

Commit and push the `Bonus/` directory with:
- ✅ `prompt.md` (unchanged)
- ✅ All 4 output files populated
- ✅ `Benchmarking_Report.md` completed

## Critical Requirements

⚠️ **DO:**
- ✅ Use the **exact same prompt** for all 4 experiments
- ✅ Run each model **independently** (no cross-contamination)
- ✅ Document **actual outputs** (no simulation unless clearly stated)
- ✅ Keep prompt.md **unchanged** across all runs

❌ **DON'T:**
- ❌ Modify the prompt between experiments
- ❌ Mix output from multiple models in one file
- ❌ Cherry-pick best results

## Why This Structure Proves Controlled Benchmarking

✅ **Same prompt** → Eliminates prompt engineering bias  
✅ **Same smell** → Consistent problem definition  
✅ **Same constraints** → Fair comparison  
✅ **Only model and mode differ** → Isolated variables

This is **empirical** (controlled experiment), not **anecdotal** (random comparisons).

## Grading Rubric

| Criterion | Points |
|-----------|--------|
| Used single fixed prompt | 2 |
| All 4 output files populated | 3 |
| Smell identification comparison completed | 2 |
| Refactoring quality analysis completed | 3 |
| Agentic vs non-agentic observations documented | 2 |
| Overall verdict with comparison to manual refactoring | 3 |
| **Total** | **15** |
