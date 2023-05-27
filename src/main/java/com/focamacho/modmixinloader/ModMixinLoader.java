package com.focamacho.modmixinloader;

import com.focamacho.modmixinloader.util.ModHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.SortingIndex(2)
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ModMixinLoader implements IFMLLoadingPlugin {

    private final Logger logger = LogManager.getLogger("ModMixinLoader");

    public ModMixinLoader() {
        MixinBootstrap.init();

        ModHandler.getMixinsToLoad().forEach((mixin, mods) -> {
            logger.info(String.format("Trying to load mixin: %s", mixin));

            for (String mod : mods) {
                if(!ModHandler.load(mod)) {
                    logger.info(String.format("Required mod %s not found. Skipping.", mod));
                    return;
                }
            }

            Mixins.addConfiguration(mixin);
            logger.info(String.format("Loaded mixin %s.", mixin));
        });

        ModHandler.clear();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
