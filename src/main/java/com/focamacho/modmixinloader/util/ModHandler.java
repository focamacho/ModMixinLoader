package com.focamacho.modmixinloader.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModHandler {

    private static final HashMap<String, File> cachedMods = new HashMap<>();
    // Mixin path - Required mods
    private static final HashMap<String, String[]> mixinsToLoad = new HashMap<>();

    static {
        String command = System.getProperty("sun.java.command");
        if(command != null) {
            Optional<String> mods = Arrays.stream(command.split(" ")).filter(it -> it.startsWith("--mods=")).findFirst();

            if (mods.isPresent()) {
                String toCheck = mods.get().substring("--mods=".length());

                for (String modFile : toCheck.split(",")) {
                    if (modFile.endsWith(".jar")) {
                        cacheModFile(new File(modFile));
                    }
                }
            }
        }

        File folder = new File("mods");
        scan(folder);
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
