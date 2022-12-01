package mekanism.client.render;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Program.Type;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import mekanism.common.Mekanism;
import mekanism.common.lib.FieldReflectionHelper;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.blending.AlphaTest;
import net.coderbot.iris.gl.blending.BlendModeOverride;
import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.gl.shader.ShaderType;
import net.coderbot.iris.gl.uniform.DynamicUniformHolder;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.newshader.CoreWorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.ExtendedShader;
import net.coderbot.iris.pipeline.newshader.FogMode;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.ShaderAttributeInputs;
import net.coderbot.iris.pipeline.newshader.ShaderKey;
import net.coderbot.iris.pipeline.newshader.TriforcePatcher;
import net.coderbot.iris.rendertarget.RenderTargets;
import net.coderbot.iris.shaderpack.DimensionId;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.uniforms.CommonUniforms;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import net.coderbot.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Mekanism.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MekanismShaders {

    static final ShaderTracker MEKASUIT = new ShaderTracker() {

        private final Map<ShaderKey, MekExtendedShader> cachedShaders = new HashMap<>();

        @Override
        public ShaderInstance get() {
            if (Iris.getPipelineManager().getPipelineNullable() instanceof CoreWorldRenderingPipeline corePipeline) {
                ShaderKey key = null;
                if (ShadowRenderer.ACTIVE) {
                    // TODO: Wrong program
                    //key = ShaderKey.SHADOW_ENTITIES_CUTOUT;//TODO: Figure out?? Potentially just return the source shader for this directly?
                    return corePipeline.getShaderMap().getShader(ShaderKey.SHADOW_ENTITIES_CUTOUT);
                } else if (HandRenderer.INSTANCE.isActive()) {
                    key = HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE;
                } else if (corePipeline.getPhase() == WorldRenderingPhase.BLOCK_ENTITIES) {
                    //key = ShaderKey.BLOCK_ENTITY_DIFFUSE;
                    Mekanism.logger.info("Rendering as block entity?");
                } else if (corePipeline.shouldOverrideShaders()) {
                    key = ShaderKey.ENTITIES_CUTOUT_DIFFUSE;
                }
                if (key != null) {
                    MekExtendedShader shader = null;
                    if (cachedShaders.containsKey(key)) {
                        shader = cachedShaders.get(key);
                        if (shader == null) {
                            return super.get();
                        } else if (!shader.closed) {
                            return shader;
                        }
                    }
                    try {
                        shader = registerOculusShader(Mekanism.rl("rendertype_mekasuit_" + key.getName()), provider, DefaultVertexFormat.NEW_ENTITY, key,
                              IrisProgramResourceFactory::patchMekaSuit);
                    } catch (IOException e) {
                        Mekanism.logger.error("Unable to build render", e);
                    }
                    cachedShaders.put(key, shader);
                    if (shader != null) {
                        return shader;
                    }
                }
            }
            return super.get();
        }
    };
    //Merge of position_color_tex and rendertype_lightning
    static final ShaderTracker SPS = new ShaderTracker() {

        private final Map<ShaderKey, MekExtendedShader> cachedShaders = new HashMap<>();

        @Override
        public ShaderInstance get() {
            if (Iris.getPipelineManager().getPipelineNullable() instanceof CoreWorldRenderingPipeline corePipeline) {
                ShaderKey key = null;
                if (ShadowRenderer.ACTIVE) {
                    //key = ShaderKey.SHADOW_LIGHTNING;//TODO: Figure out??
                    //key = ShaderKey.SHADOW_TEX_COLOR;
                } else if (corePipeline.shouldOverrideShaders()) {
                    //key = ShaderKey.LIGHTNING;
                    key = ShaderKey.TEXTURED_COLOR;
                }
                if (key != null) {
                    MekExtendedShader shader = null;
                    if (cachedShaders.containsKey(key)) {
                        shader = cachedShaders.get(key);
                        if (shader == null) {
                            return super.get();
                        } else if (!shader.closed) {
                            return shader;
                        }
                    }
                    try {
                        shader = registerOculusShader(Mekanism.rl("rendertype_sps_" + key.getName()), provider, DefaultVertexFormat.POSITION_COLOR_TEX, key,
                              IrisProgramResourceFactory::patchSPS);
                    } catch (IOException e) {
                        Mekanism.logger.error("Unable to build render", e);
                    }
                    cachedShaders.put(key, shader);
                    if (shader != null) {
                        return shader;
                    }
                }
            }
            return super.get();
        }
    };

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) throws IOException {
        registerShader(event, Mekanism.rl("rendertype_mekasuit"), DefaultVertexFormat.NEW_ENTITY, MEKASUIT);
        registerShader(event, Mekanism.rl("rendertype_sps"), DefaultVertexFormat.POSITION_COLOR_TEX, SPS);
    }

    private static void registerShader(RegisterShadersEvent event, ResourceLocation shaderLocation, VertexFormat vertexFormat, ShaderTracker tracker) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceManager(), shaderLocation, vertexFormat), tracker::setInstance);
        tracker.provider = event.getResourceManager();//TODO
    }

    /*private static final FieldReflectionHelper<ExtendedShader, GlFramebuffer> BASELINE =
          new FieldReflectionHelper<>(ExtendedShader.class, "baseline", () -> null);*/
    private static final FieldReflectionHelper<ExtendedShader, GlFramebuffer> BEFORE_TRANSLUCENT =
          new FieldReflectionHelper<>(ExtendedShader.class, "writingToBeforeTranslucent", () -> null);
    private static final FieldReflectionHelper<ExtendedShader, GlFramebuffer> AFTER_TRANSLUCENT =
          new FieldReflectionHelper<>(ExtendedShader.class, "writingToAfterTranslucent", () -> null);

    private static final FieldReflectionHelper<NewWorldRenderingPipeline, GlFramebuffer> BASELINE =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "baseline", () -> null);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, RenderTargets> RENDER_TARGETS =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "renderTargets", () -> null);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, FrameUpdateNotifier> UPDATE_NOTIFIER =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "updateNotifier", () -> null);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, ImmutableSet<Integer>> FLIPPED_AFTER_PREPARE =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "flippedAfterPrepare", ImmutableSet::of);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, ImmutableSet<Integer>> FLIPPED_AFTER_TRANSLUCENT =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "flippedAfterTranslucent", ImmutableSet::of);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, Set<ShaderInstance>> LOADED_SHADERS =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "loadedShaders", HashSet::new);
    private static final FieldReflectionHelper<NewWorldRenderingPipeline, Boolean> IS_BEFORE_TRANSLUCENT =
          new FieldReflectionHelper<>(NewWorldRenderingPipeline.class, "isBeforeTranslucent", () -> true);

    private static MekExtendedShader registerOculusShader(ResourceLocation shaderLocation, ResourceProvider resourceProvider, VertexFormat vertexFormat, ShaderKey key,
          @Nullable TriFunction<ResourceProvider, ShaderKey, ResourceLocation, ResourceProvider> patcher) throws IOException {
        if (!(Iris.getPipelineManager().getPipelineNullable() instanceof NewWorldRenderingPipeline parent)) {
            Mekanism.logger.error("Pipeline isn't initialized yet, find a different way");
            return null;
        }
        //TODO: Proper program set?
        Optional<ProgramSet> programSet = Iris.getCurrentPack().map(pack -> pack.getProgramSet(DimensionId.OVERWORLD));
        if (programSet.isEmpty()) {
            Mekanism.logger.error("Program set isn't initialized yet, find a different way");
            return null;
        }
        ProgramSource source = programSet.get().get(key.getProgram()).orElse(null);
        if (source == null) {
            Mekanism.logger.error("Program Source could not be found, find a different way");
            return null;
        }
        FogMode fogMode = key.getFogMode();
        boolean isFullbright = key.shouldIgnoreLightmap();
        AlphaTest alpha = source.getDirectives().getAlphaTestOverride().orElse(key.getAlphaTest());
        BlendModeOverride blendModeOverride = source.getDirectives().getBlendModeOverride();
        ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright);

        //TODO: Allow geometry??
        //ResourceProvider shaderResourceFactory = IrisProgramResourceFactory.read(resourceProvider, shaderLocation);
        ResourceProvider shaderResourceFactory;
        if (patcher == null) {//TODO: Improve and make this get patched at runtime
            shaderResourceFactory = IrisProgramResourceFactory.read(resourceProvider, shaderLocation);
        } else {
            //TODO: Better way to do this??
            String geometry = null;
            boolean hasGeometry = false;
            if (source.getGeometrySource().isPresent()) {
                hasGeometry = true;
                geometry = TriforcePatcher.patchVanilla(source.getGeometrySource().get(), ShaderType.GEOMETRY, alpha, true, inputs, true);
            }

            String vertex = TriforcePatcher.patchVanilla(source.getVertexSource().orElseThrow(RuntimeException::new), ShaderType.VERTEX, alpha, true, inputs, hasGeometry);
            String fragment = TriforcePatcher.patchVanilla(source.getFragmentSource().orElseThrow(RuntimeException::new), ShaderType.FRAGMENT, alpha, true, inputs, hasGeometry);

            String shaderJson = """
                  {
                      "blend": {
                          "func": "add",
                          "srcrgb": "srcalpha",
                          "dstrgb": "1-srcalpha"
                      },
                      "vertex": "placeholder",
                      "fragment": "placeholder",
                      "attributes": [
                          "Position",
                          "Color",
                          "UV0",
                          "UV1",
                          "UV2",
                          "Normal"
                      ],
                      "samplers": [
                          { "name": "gtexture" },
                          { "name": "texture" },
                          { "name": "tex" },
                          { "name": "iris_overlay" },
                          { "name": "lightmap" },
                          { "name": "normals" },
                          { "name": "specular" },
                          { "name": "shadow" },
                          { "name": "watershadow" },
                          { "name": "shadowtex0" },
                          { "name": "shadowtex1" },
                          { "name": "depthtex0" },
                          { "name": "depthtex1" },
                          { "name": "noisetex" },
                          { "name": "colortex0" },
                          { "name": "colortex1" },
                          { "name": "colortex2" },
                          { "name": "colortex3" },
                          { "name": "gaux1" },
                          { "name": "colortex4" },
                          { "name": "gaux2" },
                          { "name": "colortex5" },
                          { "name": "gaux3" },
                          { "name": "colortex6" },
                          { "name": "gaux4" },
                          { "name": "colortex7" },
                          { "name": "colortex8" },
                          { "name": "colortex9" },
                          { "name": "colortex10" },
                          { "name": "colortex11" },
                          { "name": "colortex12" },
                          { "name": "colortex13" },
                          { "name": "colortex14" },
                          { "name": "colortex15" },
                          { "name": "shadowcolor" },
                          { "name": "shadowcolor0" },
                          { "name": "shadowcolor1" }
                      ],
                      "uniforms": [
                          { "name": "iris_TextureMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                          { "name": "iris_ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                          { "name": "iris_ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                          { "name": "iris_ChunkOffset", "type": "float", "count": 3, "values": [ 0.0, 0.0, 0.0 ] },
                          { "name": "iris_ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                          { "name": "iris_FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                          { "name": "iris_FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                          { "name": "iris_LineWidth", "type": "float", "count": 1, "values": [ 1.0 ] },
                          { "name": "iris_ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] },
                          { "name": "iris_FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] }
                      ]
                  }""";

            shaderResourceFactory = patcher.apply(new IrisProgramResourceFactory(shaderJson, vertex, geometry, fragment), key, shaderLocation);

            if (!FMLLoader.isProduction() && shaderResourceFactory instanceof IrisProgramResourceFactory factory) {//TODO: Remove
                final Path debugOutDir = FMLPaths.GAMEDIR.get().resolve("patched_shaders");
                try {
                    Files.writeString(debugOutDir.resolve(shaderLocation.getPath() + ".vsh"), factory.vertex);
                    Files.writeString(debugOutDir.resolve(shaderLocation.getPath() + ".fsh"), factory.fragment);
                    if (factory.geometry != null) {
                        Files.writeString(debugOutDir.resolve(shaderLocation.getPath() + ".gsh"), factory.geometry);
                    }
                    Files.writeString(debugOutDir.resolve(shaderLocation.getPath() + ".json"), factory.json);
                } catch (IOException e) {
                }
            }
        }
        if (shaderResourceFactory == null) {
            Mekanism.logger.error("Failed to read resources");
            return null;
        }

        //TODO: Test, seems to work but still seems to modify same buffers
        RenderTargets renderTargets = RENDER_TARGETS.getValue(parent);
        if (renderTargets == null) {
            Mekanism.logger.error("Failed to get render targets");
            return null;
        }
        ImmutableSet<Integer> flippedAfterPrepare = FLIPPED_AFTER_PREPARE.getValue(parent);
        ImmutableSet<Integer> flippedAfterTranslucent = FLIPPED_AFTER_TRANSLUCENT.getValue(parent);
        GlFramebuffer writingToBeforeTranslucent = renderTargets.createGbufferFramebuffer(flippedAfterPrepare, source.getDirectives().getDrawBuffers());
        GlFramebuffer writingToAfterTranslucent = renderTargets.createGbufferFramebuffer(flippedAfterTranslucent, source.getDirectives().getDrawBuffers());
        GlFramebuffer baseline = BASELINE.getValue(parent);
        //ExtendedShader shader = (ExtendedShader) parent.getShaderMap().getShader(key);
        //TODO: Crashes on adjusting shader settings and stuff
        //GlFramebuffer writingToBeforeTranslucent = BEFORE_TRANSLUCENT.getValue(shader);
        //GlFramebuffer writingToAfterTranslucent = AFTER_TRANSLUCENT.getValue(shader);
        //GlFramebuffer baseline = BASELINE.getValue(shader);
        FrameUpdateNotifier updateNotifier = UPDATE_NOTIFIER.getValue(parent);

        MekExtendedShader newShader = new MekExtendedShader(shaderResourceFactory, shaderLocation.toString(), vertexFormat, writingToBeforeTranslucent, writingToAfterTranslucent,
              baseline, blendModeOverride, alpha, uniforms -> {
            CommonUniforms.addCommonUniforms(uniforms, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier, fogMode);
            BuiltinReplacementUniforms.addBuiltinReplacementUniforms(uniforms);
        }, isFullbright, parent, inputs);
        LOADED_SHADERS.getValue(parent).add(newShader);
        //TODO: Remainder of NewWorldRenderingPipeline#createShader code?
        Supplier<ImmutableSet<Integer>> flipped = () -> IS_BEFORE_TRANSLUCENT.getValue(parent) ? flippedAfterPrepare : flippedAfterTranslucent;
        Method addGbufferOrShadowSamplers = ObfuscationReflectionHelper.findMethod(NewWorldRenderingPipeline.class, "addGbufferOrShadowSamplers", ExtendedShader.class, Supplier.class, Boolean.TYPE);
        try {
            addGbufferOrShadowSamplers.invoke(parent, newShader, flipped, false);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Mekanism.logger.error("Failed to add gbuffer samplers");
        }
        return newShader;
    }

    private static class MekExtendedShader extends ExtendedShader {

        private boolean closed;

        public MekExtendedShader(ResourceProvider resourceFactory, String string, VertexFormat vertexFormat, GlFramebuffer writingToBeforeTranslucent,
              GlFramebuffer writingToAfterTranslucent, GlFramebuffer baseline, BlendModeOverride blendModeOverride, AlphaTest alphaTest,
              Consumer<DynamicUniformHolder> uniformCreator, boolean isIntensity, NewWorldRenderingPipeline parent, ShaderAttributeInputs inputs) throws IOException {
            super(resourceFactory, string, vertexFormat, writingToBeforeTranslucent, writingToAfterTranslucent, baseline, blendModeOverride, alphaTest, uniformCreator,
                  isIntensity, parent, inputs);
        }

        @Override
        public void close() {
            super.close();
            closed = true;
        }
    }

    private record IrisProgramResourceFactory(String json, String vertex, String geometry, String fragment) implements ResourceProvider {

        public static ResourceProvider read(ResourceProvider resourceProvider, ResourceLocation shaderLocation) {
            ResourceLocation resourcelocation = new ResourceLocation(shaderLocation.getNamespace(), "shaders/core/" + shaderLocation.getPath() + ".json");

            String json = readFile(resourceProvider, resourcelocation);
            String vertexShader;
            String geometryShader = null;//TODO: Add support for this?
            String fragmentShader;
            try (BufferedReader reader = resourceProvider.openAsReader(resourcelocation)) {
                JsonObject jsonobject = GsonHelper.parse(reader);
                vertexShader = readFile(resourceProvider, jsonobject, Program.Type.VERTEX);
                fragmentShader = readFile(resourceProvider, jsonobject, Program.Type.FRAGMENT);
            } catch (Exception e) {
                Mekanism.logger.error("Failed to read shader json", e);
                return null;
            }
            return new IrisProgramResourceFactory(json, vertexShader, geometryShader, fragmentShader);
        }

        public static ResourceProvider patchMekaSuit(ResourceProvider resourceProvider, ShaderKey key, ResourceLocation shaderLocation) {
            String json;
            String geometryShader = null;//TODO: Add support for this?

            String colorVariable = null;
            //TODO: Skip lines that are comments for purposes of checking for matches via contains?

            //TODO: Needs to be read from the shader's resource provider?
            List<String> vertexLines = readFile(resourceProvider, key, Type.VERTEX);
            if (!vertexLines.isEmpty()) {
                //TODO: Validate we have a first line/a defines?
                vertexLines.addAll(1, List.of(
                      "#define MEKANISM_NO_COLOR vec4(1, 1, 1, 1)",
                      "out vec4 mekShaderColor;",
                      "out vec2 mekUV2;"
                ));
                String glColorFunc = null;
                for (int i = 0, lines = vertexLines.size(); i < lines; i++) {
                    String line = vertexLines.get(i);
                    if (line.contains("iris_Color") && !line.startsWith("in vec4")) {
                        //Fixes setting the iris vertex color and also the defined gl_Color
                        line = line.replaceAll("\\biris_Color\\b", "MEKANISM_NO_COLOR");
                        vertexLines.set(i, line);
                    }
                    if (glColorFunc == null && line.startsWith("#define gl_Color ")) {
                        glColorFunc = line.substring("#define gl_Color ".length())
                              .replaceAll("MEKANISM_NO_COLOR", "mekTint");
                    } else if (colorVariable == null && line.contains("= gl_Color;")) {
                        colorVariable = line.replace("= gl_Color;", "").trim();
                        List<String> toAdd = List.of(
                              "vec4 mekTint = vec4(mix(MEKANISM_NO_COLOR.rgb, iris_Color.rgb, iris_Color.a), MEKANISM_NO_COLOR.a);",
                              "mekShaderColor = " + glColorFunc + ";",
                              "mekUV2 = iris_UV2;"
                        );
                        vertexLines.addAll(i + 1, toAdd);
                        lines = vertexLines.size();
                        i += toAdd.size();
                    }
                }
            } else {
                Mekanism.logger.error("Failed to patch vertex shader");
                return null;
            }
            if (colorVariable == null) {
                Mekanism.logger.error("Failed to find color variable");
                //TODO: Assume it is "color"??
                return null;
            }

            //TODO: Needs to be read from the shader's resource provider?
            List<String> fragmentLines = readFile(resourceProvider, key, Type.FRAGMENT);
            if (!fragmentLines.isEmpty()) {
                boolean addedVariables = false;
                boolean foundMain = false;
                boolean foundColorVar = false;
                boolean hasMaterialIDs = false;
                for (int i = 0, lines = fragmentLines.size(); i < lines; i++) {
                    String line = fragmentLines.get(i);
                    if (!addedVariables && line.equals("in vec4 iris_vertexColor;")) {
                        List<String> toAdd = List.of(
                              "in vec4 mekShaderColor;",
                              "in vec2 mekUV2;"
                        );
                        fragmentLines.addAll(i + 1, toAdd);
                        lines = fragmentLines.size();
                        i += toAdd.size();
                        addedVariables = true;
                        continue;
                    } else if (!hasMaterialIDs && line.equals("in float materialIDs;")) {
                        hasMaterialIDs = true;
                        continue;
                    }
                    if (foundMain) {
                        if (foundColorVar) {
                            if (line.contains(colorVariable)) {//TODO: Should we just regex it to ensure the word boundaries?
                                line = line.replaceAll("\\b" + colorVariable + "\\b", "(mekTintPixel ? mekShaderColor : " + colorVariable + ")");
                                fragmentLines.set(i, line);
                            }
                            if (line.contains("materialIDs")) {//TODO: Should we just regex it to ensure the word boundaries?
                                //SEUS
                                line = line.replaceAll("\\bmaterialIDs\\b", "mekMaterialIDs");
                                fragmentLines.set(i, line);
                            } else if (line.contains("gbuffer.materialID = ")) {
                                //SEUS specific fix, if the replace failed, and it is setting the material id, directly replace the line
                                // as the entity cutout diffuse doesn't actually take the material into account when passing to gbuffer
                                line = "    gbuffer.materialID = (mekMaterialIDs + 0.1) / 255.0;";
                                fragmentLines.set(i, line);
                            }
                            if (line.contains("emissive =")) {
                                List<String> toAdd = List.of(
                                      "if (mekMakeEmissive)",
                                      extractEmissive(line) + " = 1.0;"
                                );
                                fragmentLines.addAll(i + 1, toAdd);
                                lines = fragmentLines.size();
                                i += toAdd.size();
                            }
                        } else if (line.contains("= texture2D(texture")) {
                            boolean insertAssignment = false;
                            if (line.endsWith("* " + colorVariable + ";")) {
                                //TODO: Can we make this better
                                fragmentLines.set(i, line.replace("* " + colorVariable + ";", ";"));
                                insertAssignment = true;
                            }
                            String rawColorVar = line.substring(0, line.indexOf('=')).replaceFirst("vec4", "").trim();
                            List<String> toAdd = Lists.newArrayList(
                                  "bool mekTintPixel = mekShouldTint(" + rawColorVar + ".r, " + rawColorVar + ".g, " + rawColorVar + ".b);",
                                  "bool mekMakeEmissive = mekUV2.x > 239.0 && mekUV2.y > 239.0 && CheckForMekColor(" + rawColorVar + ".rgb, vec3(70, 242, 149));"
                            );
                            if (insertAssignment) {
                                toAdd.add(rawColorVar + " *= (mekTintPixel ? mekShaderColor : " + colorVariable + ");");
                            }
                            if (hasMaterialIDs) {
                                //SEUS SPECIFIC to make emissive actually have an effect
                                toAdd.add("float mekMaterialIDs = materialIDs;");
                                toAdd.add("if (mekMakeEmissive) {");
                                toAdd.add("    mekMaterialIDs = 32.0f;");
                                toAdd.add("}");
                            }
                            fragmentLines.addAll(i + 1, toAdd);
                            lines = fragmentLines.size();
                            i += toAdd.size();
                            foundColorVar = true;
                        }
                    } else if (line.contains("void irisMain")) {
                        List<String> toAdd = List.of(
                              "bool mekShouldTint(float red, float green, float blue) {",
                              "    float min = min(min(red, green), blue);",
                              "    float max = max(max(red, green), blue);",
                              "    float delta = max - min;",
                              "    //Calculate Saturation and Value components of HSV",
                              "    float saturation = max == 0 ? 0 : delta / max;",
                              "    float value = max;",
                              "    return value >= 0.48 && saturation <= 0.15;",
                              "}",
                              //TODO: Check license that complementary uses for this
                              "bool CheckForMekColor(vec3 albedo, vec3 check) { // Thanks Builderb0y",
                              "    vec3 dif = albedo - check / 255.0;",
                              "    return dif == clamp(dif, vec3(-0.001), vec3(0.001));",
                              "}"
                        );
                        fragmentLines.addAll(i, toAdd);
                        lines = fragmentLines.size();
                        i += toAdd.size();
                        foundMain = true;
                    }
                }
            } else {
                Mekanism.logger.error("Failed to patch fragment shader");
                return null;
            }

            //TODO: Needs to be read from the shader's resource provider?
            try (BufferedReader reader = resourceProvider.openAsReader(new ResourceLocation(key.getName() + ".json"))) {
                JsonObject jsonobject = GsonHelper.parse(reader);
                jsonobject.addProperty(Program.Type.VERTEX.getName(), shaderLocation.toString());
                jsonobject.addProperty(Program.Type.FRAGMENT.getName(), shaderLocation.toString());
                json = jsonobject.toString();
            } catch (Exception e) {
                Mekanism.logger.error("Failed to read shader json", e);
                return null;
            }

            return new IrisProgramResourceFactory(json,
                  vertexLines.stream().collect(Collectors.joining(System.lineSeparator())),
                  geometryShader,
                  fragmentLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
        }

        public static ResourceProvider patchSPS(ResourceProvider resourceProvider, ShaderKey key, ResourceLocation shaderLocation) {
            String json;
            String geometryShader = null;//TODO: Add support for this?
            //TODO: Skip lines that are comments for purposes of checking for matches via contains?

            List<String> vertexLines = readFile(resourceProvider, key, Type.VERTEX);
            if (vertexLines.isEmpty()) {
                Mekanism.logger.error("Failed to patch vertex shader");
                return null;
            }//TODO: Do we need to patch this?

            List<String> fragmentLines = readFile(resourceProvider, key, Type.FRAGMENT);
            if (!fragmentLines.isEmpty()) {
                boolean foundMain = false;
                for (int i = 0, lines = fragmentLines.size(); i < lines; i++) {
                    String line = fragmentLines.get(i);
                    if (foundMain) {
                        if (line.contains("emissive =")) {
                            fragmentLines.add(++i, extractEmissive(line) + " = 1.0;");
                            lines++;
                        }
                    } else if (line.contains("void irisMain")) {
                        foundMain = true;
                    }
                }
            } else {
                Mekanism.logger.error("Failed to patch fragment shader");
                return null;
            }

            //TODO: Needs to be read from the shader's resource provider?
            try (BufferedReader reader = resourceProvider.openAsReader(new ResourceLocation(key.getName() + ".json"))) {
                JsonObject jsonobject = GsonHelper.parse(reader);
                jsonobject.addProperty(Program.Type.VERTEX.getName(), shaderLocation.toString());
                jsonobject.addProperty(Program.Type.FRAGMENT.getName(), shaderLocation.toString());
                json = jsonobject.toString();
            } catch (Exception e) {
                Mekanism.logger.error("Failed to read shader json", e);
                return null;
            }

            return new IrisProgramResourceFactory(json,
                  vertexLines.stream().collect(Collectors.joining(System.lineSeparator())),
                  geometryShader,
                  fragmentLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
        }

        private static String extractEmissive(String line) {
            String start = line.substring(0, line.indexOf("emissive ="))
                  .replaceFirst("\\bfloat\\b", "");
            char[] chars = start.toCharArray();
            int varStart = 0;
            for (int j = chars.length - 1; j >= 0; j--) {
                char c = chars[j];
                if (c == ',' || c == ';') {
                    varStart = j + 1;
                }
            }
            return start.substring(varStart) + "emissive";
        }

        private static List<String> readFile(ResourceProvider resourceProvider, ShaderKey key, Program.Type type) {
            Optional<Resource> resource = resourceProvider.getResource(new ResourceLocation(key.getName() + type.getExtension()));
            if (resource.isPresent()) {
                try (BufferedReader reader = resource.get().openAsReader()) {
                    //TODO: Figure out a better way to ignore comments
                    return reader.lines().filter(line -> !line.isBlank() && !line.trim().startsWith("//")).collect(Collectors.toList());
                } catch (Exception e) {
                    Mekanism.logger.error("Failed to read " + type.getName() + " shader", e);
                }
            }
            Mekanism.logger.error("Failed to find " + type.getName() + " shader");
            return Collections.emptyList();
        }

        private static String readFile(ResourceProvider resourceProvider, JsonObject jsonobject, Program.Type type) {
            ResourceLocation rl = new ResourceLocation(GsonHelper.getAsString(jsonobject, type.getName()));
            return readFile(resourceProvider, new ResourceLocation(rl.getNamespace(), "shaders/core/" + rl.getPath() + type.getExtension()));
        }

        private static String readFile(ResourceProvider resourceProvider, ResourceLocation file) {
            try (BufferedReader reader = resourceProvider.openAsReader(file)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            } catch (Exception e) {
                Mekanism.logger.error("Failed to read shader " + file, e);
            }
            return null;
        }

        @NotNull
        @Override
        public Optional<Resource> getResource(ResourceLocation id) {
            String path = id.getPath();
            if (path.endsWith("json")) {
                return Optional.of(new StringResource(id, this.json));
            } else if (path.endsWith("vsh")) {
                return Optional.of(new StringResource(id, this.vertex));
            } else if (path.endsWith("gsh")) {
                return this.geometry == null ? Optional.empty() : Optional.of(new StringResource(id, this.geometry));
            }
            return path.endsWith("fsh") ? Optional.of(new StringResource(id, this.fragment)) : Optional.empty();
        }

        private static class StringResource extends Resource {

            private StringResource(ResourceLocation id, String content) {
                super("<iris shaderpack shaders>", () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    static class ShaderTracker implements Supplier<ShaderInstance> {

        ResourceProvider provider;//TODO
        private ShaderInstance instance;
        final ShaderStateShard shard = new ShaderStateShard(this);

        private ShaderTracker() {
        }

        private void setInstance(ShaderInstance instance) {
            this.instance = instance;
        }

        @Override
        public ShaderInstance get() {
            return instance;
        }
    }
}