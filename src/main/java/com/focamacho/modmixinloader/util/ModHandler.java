package com.focamacho.modmixinloader.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModHandler {

    private static final HashMap<String, File> cachedMods = new HashMap<>();
    // Mixin path - Required mods
    private static final HashMap<String, String[]> mixinsToLoad = new HashMap<>();

    static {
        if(MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.SERVER) {
            scan(new File("./mods"));
        } else {
            // Load mods from --mods arg
            String command = System.getProperty("sun.java.command");
            String gameFolder = null;
            if (command != null) {
                try {
                    String mods = command.substring(command.indexOf("--mods=") + 7);

                    for (String modFile : mods.split(",")) {
                        if (modFile.endsWith(".jar")) {
                            cacheModFile(new File(modFile));
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }

                try {
                    gameFolder = command.substring(command.indexOf("--gameDir") + 10).split(" ")[0];
                } catch (IndexOutOfBoundsException ignored) {
                }
            }

            // Load mods from mod_list.json
            File modList = new File("mods/mod_list.json");
            if (modList.exists()) {
                try {
                    JsonObject json = new GsonBuilder().create().fromJson(new FileReader(modList), JsonObject.class);

                    if (json.has("repositoryRoot")) {
                        File modFolder = new File(json.get("repositoryRoot").getAsString());
                        JsonArray mods = json.getAsJsonArray("modRef");
                        for (JsonElement mod : mods) {
                            String[] split = mod.getAsString().split(":");
                            if (split.length != 3) continue;
                            File modLocation = new File(
                                    modFolder,
                                    split[0].replace(".", "/") + "/" + split[1] + "/" + split[2] + "/"
                            );

                            if (modLocation.exists()) {
                                File[] files = modLocation.listFiles();
                                if (files != null && files.length > 0) {
                                    cacheModFile(files[0]);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String modsFolder = System.getProperty("LibLoader.modsFolder");
            if (modsFolder != null && !modsFolder.isEmpty()) scan(new File(modsFolder));
            else {
                if (gameFolder == null) gameFolder = System.getProperty("user.dir");
                File folder = gameFolder != null && !gameFolder.isEmpty() ? new File(gameFolder, "mods") : new File("mods");
                scan(folder);
            }
        }
    }

    private static void scan(File folder) {
        if(folder.exists() && folder.isDirectory() && folder.listFiles() != null) {
            File[] mods = folder.listFiles(f -> f.getName().endsWith(".jar") || f.isDirectory());
            if(mods != null) {
                for (File mod : mods) {
                    if(mod.isDirectory() && mod.getName().equals("1.12.2")){
                        scan(mod);
                    }

                    cacheModFile(mod);
                }
            }
        }
    }

    private static void cacheModFile(File file) {
        try(ZipFile zip = new ZipFile(file)) {
            List<? extends ZipEntry> entries = zip.stream().filter(entry -> entry != null && !entry.isDirectory() &&
                    (entry.getName().equals("mcmod.info") || entry.getName().startsWith("modmixins/"))).collect(Collectors.toList());

            for (ZipEntry entry : entries) {
                if(entry.getName().equals("mcmod.info")) {
                    try(InputStream is = zip.getInputStream(entry)) {
                        try(InputStreamReader isr = new InputStreamReader(is)) {
                            try(BufferedReader reader = new BufferedReader(isr)) {
                                JsonArray array = new GsonBuilder().create()
                                        .fromJson(reader, JsonArray.class);

                                if(array.size() > 0) {
                                    String modid = array.get(0).getAsJsonObject().get("modid").getAsString();
                                    if(modid != null) {
                                        cachedMods.put(modid, file);
                                    }
                                }
                            }
                        }
                    }
                } else if(entry.getName().startsWith("modmixins/")) {
                    try(InputStream is = zip.getInputStream(entry)) {
                        try(InputStreamReader isr = new InputStreamReader(is)) {
                            try(BufferedReader reader = new BufferedReader(isr)) {
                                JsonObject object = new GsonBuilder().create()
                                        .fromJson(reader, JsonObject.class);

                                JsonArray requiredMods = object.get("mods").getAsJsonArray();
                                if(requiredMods.size() > 0) {
                                    String[] mods = new String[requiredMods.size()];
                                    for(int i = 0; i < requiredMods.size(); i++) {
                                        mods[i] = requiredMods.get(i).getAsString();
                                    }

                                    mixinsToLoad.put(entry.getName(), mods);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static boolean load(String modid) {
        try {
            if (cachedMods.containsKey(modid)) {
                loadJar(cachedMods.get(modid));
                return true;
            }
        } catch (MalformedURLException ignored) {}

        return false;
    }

    private static void loadJar(File jar) throws MalformedURLException {
        ((LaunchClassLoader) ModHandler.class.getClassLoader()).addURL(jar.toURI().toURL());
        CoreModManager.getReparseableCoremods().add(jar.getName());
    }

    public static Map<String, String[]> getMixinsToLoad() {
        return Collections.unmodifiableMap(mixinsToLoad);
    }

    public static void clear() {
        cachedMods.clear();
        mixinsToLoad.clear();
    }

}
