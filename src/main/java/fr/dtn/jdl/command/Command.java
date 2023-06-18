package fr.dtn.jdl.command;

import com.moandjiezana.toml.Toml;
import fr.dtn.jdl.Bot;
import fr.dtn.jll.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents a raw text command
 */
public class Command{
    /**
     * The display name and description of the command
     */
    private final String displayName, description;
    /**
     * The calls of the command, or all the keywords that can be used to call the command
     */
    private final String[] calls;
    /**
     * Permissions required to use the command
     */
    private final Permission[] permissions;
    /**
     * Executor of the command
     */
    private CommandExecutor executor;

    /**
     * Constructor
     * @param file The configuration file that contains information of the command
     */
    public Command(File file){
        Log.info("Loading command from file '" + file.getPath() + "'");
        Toml toml = new Toml().read(file);

        if(file.getName().contains(" "))
            throw new RuntimeException("Command name cannot contain spaces.");

        this.displayName = file.getName().replace(".toml", "");
        this.description = toml.getString("description");
        List<String> calls = new ArrayList<>();
        toml.getList("calls").forEach(call -> calls.add(call.toString()));
        this.calls = calls.toArray(new String[0]);

        List<Object> perms = toml.getList("permissions");
        this.permissions = new Permission[perms.size()];

        for(int i = 0; i < permissions.length; i++){
            String permission = perms.get(i).toString().toUpperCase();

            try{
                permissions[i] = Permission.valueOf(permission);
            }catch(IllegalArgumentException e){
                Log.error("Loading command '" + displayName + "' : Failed to load permission '" + permission + "' : Permission does not exist");
            }
        }


        String className = toml.getString("executor");
        try {
            this.executor = (CommandExecutor) Class.forName(className).getConstructor().newInstance();
        }catch(ClassNotFoundException e){
            Log.error("Loading command '" + displayName + "' failed : Impossible to load executor class '" + className + "' : Class not found");
            return;
        }catch(NoSuchMethodException e){
            Log.error("Loading command '" + displayName + "' failed : Class '" + className + "' does not have an argument-less constructor");
            return;
        }catch(InvocationTargetException | InstantiationException e){
            Log.error("Loading command '" + displayName + "' failed : Class '" + className + "' cannot be instantiated : Unknown reason");
            return;
        }catch(IllegalAccessException e){
            Log.error("Loading command '" + displayName + "' failed : Class '" + className + "' argument-less constructor is not public");
            return;
        }

        Log.info("Command '" + displayName + "' loaded successfully");
    }

    /**
     * To execute the command
     * @param bot The current bot
     * @param guild The guild where the command is used
     * @param channel The text channel where the command is used
     * @param message The message sent detected as this command call
     * @param author The author of the command
     * @param member The author of the command as member of the guild
     * @param args The arguments entered with the command
     */
    public void execute(Bot bot, Guild guild, TextChannel channel, Message message, User author, Member member, String[] args) {
        executor.execute(bot, guild, channel, message, author, member, args);
    }

    /**
     * To get display name
     * @return Display name
     */
    public String getDisplayName() { return displayName; }

    /**
     * To get description
     * @return Description
     */
    public String getDescription() { return description; }

    /**
     * To get calls
     * @return Calls
     */
    public String[] getCalls() { return calls; }

    /**
     * To get permissions
     * @return Permissions
     */
    public Permission[] getPermissions() { return permissions; }
}