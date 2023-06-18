package fr.dtn.jdl.command;

import fr.dtn.jdl.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Represent the function that will be called when a command is used
 */
public interface CommandExecutor {
    /**
     * To execute the command
     * @param bot The current bot
     * @param guild The guild where the command is used
     * @param channel The text channel where the command is used
     * @param message The message that called the command
     * @param author The author of the message
     * @param member The author of the message as a member of the guild
     * @param args The arguments given by the author of the command
     */
    void execute(Bot bot, Guild guild, TextChannel channel, Message message, User author, Member member, String[] args);
}