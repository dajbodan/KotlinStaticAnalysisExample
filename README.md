# Project: Static Analyzer & Constant Folding

This project is a static analyzer for a simple imperative language. It demonstrates several compiler-related concepts, including:
* Parsing an AST (Abstract Syntax Tree)
* Converting an AST into a CFG (Control Flow Graph)
* Data-Flow Analysis (specifically, a "must-analysis")
* Constant Propagation and Folding optimization

## 🚀 How to Run the Demo

The main demonstration is handled by the `Main.kt` file.

1.  **Build the project:**
    * Run the Gradle `build` task (e.g., `./gradlew build` in your terminal or via the IntelliJ Gradle window).
2.  **Run the demo:**
    * Execute the `main` function in `src/main/kotlin/org/example/Main.kt`.

This will print a full report to the console, showing the original AST, the unoptimized CFG, and the final optimized CFG. It will also write the Mermaid graph output to files (like `example_optimized.mmd`) in the project root.

---

## 🧠 Solution Details & Comments

This is where you can write the explanations you wanted to put in the notebook.

### 1. Control Flow Graph (`ControlFlowGraphBuilder.kt`)

* **Approach:** I used the Visitor pattern to traverse the `Stmt` (AST) structure.
* **Algorithm:** The core logic is a single-pass, recursive algorithm using a "continuation-passing style." A private `nextNode` variable tracks what the *end* of the current statement should point to.
* **Key Logic:** `visitBlock` iterates the statement list *in reverse* to correctly chain the `nextNode` variable back from the end of the block to the beginning.

### 2. Constant Folding (`ConstantFolding.kt`)

This is the core of the analysis. It's a "must-analysis" (all-paths) implementation.

* **Worklist Algorithm:** Instead of naively looping over all nodes until the state stabilizes, I used an `ArrayDeque` as a **worklist**. This is much more efficient, as it only re-computes nodes that are successors to a node whose state has changed.
* **Merge Function:** The `merge` function is the heart of the "must-analysis." It combines the `in-states` from all a node's predecessors. If a variable has different values (e.g., `Const(1)` on one path, `Const(2)` on another) or is *undefined* on any path (`null`), it is correctly marked as `AValue.Unknown`.
* **Handling Loops:** The `merge` logic was tricky for loops. A loop's `in-state` is a merge of its "entry" path (e.g., from `x=0` before the loop) and its "back-edge" path (e.g., from `x=x+1` inside the loop). This correctly causes loop-variant variables to become `Unknown`.

### 3. Architecture

* **Visitor Pattern:** I used the Visitor pattern (`accept`/`visit`) extensively. This cleanly separates the data structures (`Expr`, `Stmt`, `Node`) from the operations performed on them (printing, building the CFG, constant folding).
* **Facade (`Controller.kt`):** The `Controller` class acts as a facade. It hides the complex, multi-step pipeline (`buildCfg`, `constantFold`, `renderMermaid`) behind a single, clean `run()` method. All internal steps are `private` to ensure proper encapsulation.
* **Flexibility (`MermaidBuilder.kt`):** The Mermaid builder was refactored to write to a generic `Appendable` interface instead of a hardcoded `StringBuilder`. This allows it to print to the console, a file, or in-memory (for tests) without changing its code.