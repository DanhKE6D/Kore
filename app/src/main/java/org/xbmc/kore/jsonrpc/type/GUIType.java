package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;

import org.xbmc.kore.utils.JsonUtils;

/**
 * Created by danh.le on 8/21/15.
 */
public class GUIType {
    /**
     * GUI.Property.Value
     */
    public static class PropertyValue {
        public static final String CURRENTWINDOW = "currentwindow";
        public static final String CURRENTCONTROL = "currentcontrol";
        public static final String SKIN = "skin";
        public static final String FULLSCREEN = "fullscreen";

        // class members
        public final CurrentWindow currentwindow;
        public final Skin skin;
        public final String currentcontrol;
        public final Boolean fullscreen;

        public PropertyValue(JsonNode node) {
            skin = new Skin(node.get(SKIN));
            fullscreen = JsonUtils.booleanFromJsonNode(node, FULLSCREEN, false);
            currentcontrol = JsonUtils.stringFromJsonNode(node, CURRENTCONTROL);
            currentwindow = new CurrentWindow(node.get(CURRENTWINDOW));
        }
    }

    public static class Skin {
        public static final String ID = "id";
        public static final String NAME = "name";

        public final String id;
        public final String name;

        public Skin(JsonNode node) {
            id = JsonUtils.stringFromJsonNode(node, ID);
            name = JsonUtils.stringFromJsonNode(node, NAME);
        }

    }

    public static class CurrentWindow {
        public static final String LABEL = "label";
        public static final String ID = "id";

        public final Integer id;
        public final String label;

        public CurrentWindow(JsonNode node) {
            id = JsonUtils.intFromJsonNode(node, ID, 0);
            label = JsonUtils.stringFromJsonNode(node, LABEL);
        }
    }
}
