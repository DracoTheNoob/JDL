package fr.dtn.jdl.event;

import fr.dtn.jdl.Bot;
import net.dv8tion.jda.api.events.GenericEvent;

/**
 * Represents the code that will be run when a certain event happen
 * @param <T> The type of the handled event
 */
public interface EventHandler<T extends GenericEvent>{
    /**
     * To execute the handler
     * @param bot The current bot
     * @param event The event that happened
     * @param <E> The type of the handled event
     */
    void happen(Bot bot, T event);
}