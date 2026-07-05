package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.gui.RewardEditorGui.EditorState;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/**
 * Represents a pending chat input request.
 * When the player types in chat (handled by GuiListener),
 * the callback is invoked with the typed value.
 */
public class ChatInputSession {

    private final String prompt;
    private final EditorState state;
    private final BiConsumer<String, EditorState> callback;

    public ChatInputSession(String prompt, EditorState state,
                            BiConsumer<String, EditorState> callback) {
        this.prompt = prompt;
        this.state = state;
        this.callback = callback;
    }

    public String getPrompt() { return prompt; }
    public EditorState getState() { return state; }

    public void complete(String input) {
        callback.accept(input, state);
    }
}
