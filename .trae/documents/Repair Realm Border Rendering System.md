# Realm Border Rendering Repair Plan

## Diagnosis

The rendering failure is caused by an improper implementation of the rendering pipeline in `RealmBoundaryRenderer.java`.

* **Root Cause**: The current implementation uses the global `BufferSource` with custom `RenderType`s and manually calls `endBatch()`. This is fragile and likely conflicts with the game's rendering state, specifically preventing the translucent walls (which use `COLOR_WRITE` only) from rendering correctly. The vertical lines work because they likely use a state that happens to coincide with the default or flushed state (`COLOR_DEPTH_WRITE`).

* **Solution**: Refactor the renderer to use "Immediate Mode" rendering via `Tesselator` and `RenderSystem`, mirroring the working implementation in `ClaimBoundaryRenderer.java`. This guarantees control over the rendering state (blending, depth mask, culling) and execution order.

## Implementation Steps

### 1. Refactor `RealmBoundaryRenderer`

I will rewrite the `renderRealmBoundary` method and its helpers to use `Tesselator` directly instead of `BufferSource`.

#### A. Setup Rendering State

* Remove `REALM_BOUNDARY_TRANSLUCENT` and `REALM_BOUNDARY_LINES` RenderTypes.

* In `onRenderLevel`, setup the common `RenderSystem` state:

  * Enable Blend (`SourceAlpha`, `OneMinusSourceAlpha`).

  * Disable Cull.

  * Enable Depth Test.

  * Set Shader to `PositionColor`.

#### B. Render Walls (Translucent Pass)

* Set `RenderSystem.depthMask(false)` to ensure transparent walls don't occlude things behind them.

* Use `Tesselator` to start a `QUADS` buffer.

* Call `renderEnhancedWall`, `renderOuterLayerBoundary`, and `renderBubbleParticles`.

* Draw the buffer immediately.

#### C. Render Lines (Solid/Depth Write Pass)

* Set `RenderSystem.depthMask(true)` (or keep false if preferred, but lines usually benefit from depth writing if they are "solid"). *Correction*: Since lines are also semi-transparent or additive, keeping `depthMask(false)` is safer for visual consistency, but `ClaimBoundaryRenderer` uses `depthMask(false)` for everything. The original code used `COLOR_DEPTH_WRITE` for lines, implying they *wanted* depth writing. I will use `depthMask(false)` for lines as well to prevent z-fighting with the walls, as they are part of the same ghost-like boundary.

* Use `Tesselator` to start a `QUADS` buffer.

* Call `renderVerticalEdges` and `renderGroundLines`.

* Draw the buffer immediately.

### 2. Implementation Details

* **`renderEnhancedWall`**: Update to accept `BufferBuilder` instead of `VertexConsumer`. Remove the `try-catch` blocks related to buffer state (since we control the builder).

* **`renderVerticalEdges`**: Update to accept `BufferBuilder`.

* **Validation**: Add logging to `renderRealmBoundary` to confirm it is running and processing realms.

### 3. Verification

* The new implementation will be structurally identical to the working `ClaimBoundaryRenderer` but with the specific visual effects (waves, bubbles) of the Realm renderer.

* This ensures "visual consistency" and "full rendering functionality".

## Verification & Error Handling

* **Error Handling**: Add checks for `Tesselator` state.

* **Validation**: Add a debug log that prints once every 5 seconds if realms are being rendered, confirming the loop is active.

