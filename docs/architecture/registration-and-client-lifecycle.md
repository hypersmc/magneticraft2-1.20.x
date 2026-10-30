# Registration and client lifecycle (Forge 1.20.1)

Magneticraft2 uses the existing Forge `DeferredRegister` classes. This cleanup separates client-only initialization from common registrations without changing saved registry IDs.

## Ownership

- `FinalRegistry` wires blocks, items, block entities, recipes, fluids, menus, entities and tabs to the mod event bus.
- `ContainerAndScreenRegistry` registers common menu types. The legacy name is retained so other code does not need renaming.
- `Clientsetup` owns client mod-bus setup. It delegates renderers, baked models and GUI screens to `ClientRenderers`, `ClientModels` and `ClientScreens`.
- `ClientModels` registers renderer-only models before model baking. Keep explicit model IDs needed on first load and after F3+T.
- `ClientScreens` binds common menus to client GUI classes in `FMLClientSetupEvent.enqueueWork`.
- Existing menu implementations under `client/gui/container` use common menu APIs. Their historical package paths are left unchanged.

## Adding a machine

Register its block, block entity and item in the existing common registries. Add its renderer to `ClientRenderers` only if required. Put renderer-only models in `ClientModels`. If it has a GUI, register the common menu type and bind its client screen in `ClientScreens`.

Keep comments short and useful: explain constraints and unexpected behavior, not the obvious method calls. Put bigger design and JSON-format explanations under `docs/`.

## Verification

Run `./gradlew build`, test dedicated-server and client startup, open each GUI screen, press F3+T, and inspect animated Copper machines and the sifter's wear models. Source checks are not a substitute for compilation or in-game testing.
