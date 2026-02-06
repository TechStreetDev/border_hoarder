/*
 * Copyright (C) 2026 TechStreetDev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techstreet.border.lib.border;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import tech.techstreet.border.lib.item.BoarderItem;
import tech.techstreet.border.lib.user.UserState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ProgressHandler {
    private static final File file = new File("world/data/border.dat");
    private static final HashMap<UUID, Location> lastLocations = new HashMap<>();
    private static final HashMap<UUID, UserState> lastStates = new HashMap<>();

    /**
     * Gzip and Base64 encode a JSON string.
     *
     * @param json The JSON string to compress and encode.
     * @return The compressed and encoded string.
     * @throws Exception If an error occurs during compression or encoding.
     */
    private static String gzipAndBase64(String json) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(byteStream)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }

        byte[] gzipped = byteStream.toByteArray();
        return Base64.getEncoder().encodeToString(gzipped);
    }

    /**
     * Decode a Base64 string and gunzip it to a JSON string.
     *
     * @param base64 The Base64 encoded and compressed string.
     * @return The decompressed JSON string.
     * @throws Exception If an error occurs during decoding or decompression.
     */
    private static String base64AndGunzip(String base64) throws Exception {
        byte[] compressed = Base64.getDecoder().decode(base64);

        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Save the world progress to a file.
     *
     * @param completedItems The list of completed BoarderItems.
     */
    public static void saveWorld(List<BoarderItem> completedItems) {
        try {
            List<String> collected = completedItems.stream()
                    .map(BoarderItem::name)
                    .toList();

            JsonObject jsonObject = new JsonObject();
            jsonObject.add("collected", new Gson().toJsonTree(collected));

            JsonObject locations = new JsonObject();
            for (UUID uuid : lastLocations.keySet()) {
                if (lastLocations.get(uuid) != null) {
                    JsonObject location = new JsonObject();
                    location.addProperty("world", lastLocations.get(uuid).getWorld().getName());
                    location.addProperty("x", lastLocations.get(uuid).getX());
                    location.addProperty("y", lastLocations.get(uuid).getY());
                    location.addProperty("z", lastLocations.get(uuid).getZ());
                    location.addProperty("pitch", lastLocations.get(uuid).getPitch());
                    location.addProperty("yaw", lastLocations.get(uuid).getYaw());
                    location.addProperty("state", lastStates.get(uuid).name());

                    locations.add(uuid.toString(), location);
                }
            }

            jsonObject.add("locations", locations);
            String jsonString = new Gson().toJson(jsonObject);
            String compressedData = gzipAndBase64(jsonString);

            Files.writeString(Path.of(file.getPath()), compressedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the world progress from a file.
     *
     * @return The list of completed BoarderItems.
     * @throws Exception If an error occurs during loading.
     */
    public static List<BoarderItem> loadWorld() throws Exception {
        List<BoarderItem> loadedItems = new ArrayList<>();
        if (!file.exists()) return loadedItems;

        JsonObject jsonObject = JsonParser.parseString(base64AndGunzip(Files.readString(file.toPath()))).getAsJsonObject();
        for (JsonElement element : jsonObject.getAsJsonArray("collected")) {
            try {
                loadedItems.add(BoarderItem.valueOf(element.getAsString()));
            } catch (Exception ignored) {
            }
        }

        JsonObject locations = jsonObject.getAsJsonObject("locations");
        if (locations != null) {
            for (String uuid : locations.keySet()) {
                try {
                    lastStates.put(UUID.fromString(uuid), UserState.valueOf(locations.getAsJsonObject(uuid).get("state").getAsString()));
                    lastLocations.put(UUID.fromString(uuid), new Location(
                            Bukkit.getWorld(locations.getAsJsonObject(uuid).get("world").getAsString()),
                            locations.getAsJsonObject(uuid).get("x").getAsDouble(),
                            locations.getAsJsonObject(uuid).get("y").getAsDouble(),
                            locations.getAsJsonObject(uuid).get("z").getAsDouble(),
                            locations.getAsJsonObject(uuid).get("yaw").getAsFloat(),
                            locations.getAsJsonObject(uuid).get("pitch").getAsFloat()
                    ));
                } catch (Exception ignored) {
                }
            }
        }

        return loadedItems;
    }

    /**
     * Update the last known location and state of a user.
     *
     * @param uuid     the UUID of the user.
     * @param location the last known location of the user.
     * @param state    the last known state of the user.
     */
    public static void updateState(UUID uuid, Location location, UserState state) {
        lastLocations.put(uuid, location);
        lastStates.put(uuid, state);
    }

    /**
     * Get the last known states of users.
     *
     * @return A map of user UUIDs to their last known UserState.
     */
    public static HashMap<UUID, UserState> getLastStates() {
        return lastStates;
    }

    /**
     * Get the last known locations of users.
     *
     * @return A map of user UUIDs to their last known Location.
     */
    public static HashMap<UUID, Location> getLastLocations() {
        return lastLocations;
    }
}