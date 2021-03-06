/*
 * This file is part of jxpl.
 *
 * jxpl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jxpl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jxpl.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.angelsl.bukkit.jxpl;

import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class JxplPlugin extends JavaPlugin {
    private static File scriptsDir = null;
    private static ArrayList<ScriptPlugin> loadedPlugins = new ArrayList<ScriptPlugin>();
    private static JxplPlugin thisPlugin;
    
    public JxplPlugin() {
        thisPlugin = this;
    }

    protected static JxplPlugin getPlugin()
    {
        return thisPlugin;
    }

    protected static File getScriptsDir() {
        return scriptsDir;
    }

    protected static ArrayList<ScriptPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }

    public void onDisable() {
        for (ScriptPlugin sp : loadedPlugins) {
            getServer().getPluginManager().disablePlugin(sp);
        }
    }

    public void onEnable() {
        if (scriptsDir == null) {
            onLoad();
        }
        for (ScriptPlugin p : loadedPlugins) {
            getServer().getPluginManager().enablePlugin(p);
        }
    }

    @Override
    public void onLoad() {
        Utils.log(Level.INFO, String.format("Initialising jxpl %s on server version \"%s\", Bukkit version \"%s\"", getDescription().getVersion(), getServer().getVersion(), getServer().getBukkitVersion()));
        if (!fixFileAssociations(getServer().getPluginManager())) {
            Utils.log(Level.SEVERE, "Unable to fix file associations. Please report this with your (Craft)Bukkit version!");
        }
        this.getServer().getPluginManager().registerInterface(ScriptLoader.class);
        scriptsDir = new File(getConfig().getString("scripts-dir", "scripts"));
        if (scriptsDir.exists() && !scriptsDir.isDirectory()) {
            scriptsDir.delete();
        }
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir();
        }
        this.getServer().getPluginManager().loadPlugins(scriptsDir);
    }

    // WARNING: DIRTY SHIT FOLLOWS.

    private static boolean fixFileAssociations(PluginManager spm) {
        if (!(spm instanceof SimplePluginManager)) {
            Utils.log(Level.WARNING, String.format("Plugin manager is not a SimplePluginManager but a \"%s\"! Refusing to fix file associations.", spm.getClass().getName()));
            return false;
        }
        HashMap<Pattern, PluginLoader> fileAssociations = (HashMap<Pattern, PluginLoader>) Utils.getFieldHelper(spm, "fileAssociations");
        HashMap<Pattern, PluginLoader> fixedAssociations = new HashMap<Pattern, PluginLoader>();
        if (fileAssociations == null) {
            return false; // probably not a SPM
        }
        ArrayList<Map.Entry<Pattern, PluginLoader>> ks = new ArrayList<Map.Entry<Pattern, PluginLoader>>(fileAssociations.entrySet()); // avoid ConcurrentModificationException... if any
        for (Map.Entry<Pattern, PluginLoader> pl : ks) {
            if (!(pl.getValue().getClass().getName().equalsIgnoreCase(ScriptLoader.class.getName()))) {
                fixedAssociations.put(pl.getKey(), pl.getValue());
            }
        }
        return Utils.setFieldHelper(spm, "fileAssociations", fixedAssociations);
    }
    // END DIRTY SHIT
}
