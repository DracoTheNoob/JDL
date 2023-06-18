package fr.dtn.jdl.slashcommand;

import com.moandjiezana.toml.Toml;
import fr.dtn.jdl.Bot;
import fr.dtn.jll.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that represents a slash command
 */
public class SlashCommand {
    /**
     * The display name and description of the slash command
     */
    private final String displayName, description;
    /**
     * The permissions required to run the slash command
     */
    private Permission[] permissions;
    /**
     * The executor of the slash command
     */
    private SlashCommandExecutor executor;
    /**
     * The parameters of the slash command
     */
    private List<Parameter> parameters;

    /**
     * Constructor
     * @param file The configuration file of the slash command
     */
    public SlashCommand(File file){
        Log.info("Loading slash command from file '" + file.getPath() + "'");
        Toml toml = new Toml().read(file);

        this.displayName = file.getName().replace(".toml", "");
        this.description = toml.getString("description");

        List<Object> perms = toml.getList("permissions");

        try{
            this.permissions = new Permission[perms.size()];
        }catch(NullPointerException e){
            Log.warn("Permissions of slash command '" + displayName + "' are not set, using default empty permissions");
            this.permissions = new Permission[0];
        }

        for(int i = 0; i < permissions.length; i++){
            String permission = perms.get(i).toString().toUpperCase();

            try{
                permissions[i] = Permission.valueOf(permission);
            }catch(IllegalArgumentException e){
                Log.error("Loading slash command '" + displayName + "' : Failed to load permission '" + permission + "' : Permission does not exist");
            }
        }


        String className = toml.getString("executor");
        try {
            this.executor = (SlashCommandExecutor) Class.forName(className).getConstructor().newInstance();
        }catch(ClassNotFoundException e){
            Log.error("Loading slash command '" + displayName + "' failed : Impossible to load executor class '" + className + "' : Class not found");
            return;
        }catch(NoSuchMethodException e){
            Log.error("Loading slash command '" + displayName + "' failed : Class '" + className + "' does not have an argument-less constructor");
            return;
        }catch(InvocationTargetException | InstantiationException e){
            Log.error("Loading slash command '" + displayName + "' failed : Class '" + className + "' cannot be instantiated : Unknown reason");
            return;
        }catch(IllegalAccessException e){
            Log.error("Loading slash command '" + displayName + "' failed : Class '" + className + "' argument-less constructor is not public");
            return;
        }

        Log.info("Loading slash command options");
        List<String> names = toml.getList("parameters.name");
        List<String> descriptions = toml.getList("parameters.description");
        List<Boolean> required = toml.getList("parameters.required");
        List<Boolean> autoCompletes = toml.getList("parameters.autoComplete");
        List<List<String>> choice = toml.getList("parameters.choice");
        List<String> typesNames = toml.getList("parameters.type");


        for(List<?> list : Arrays.asList(descriptions, required, autoCompletes, choice, typesNames)){
            if(list.size() != names.size()){
                Log.error("Loading slash command '" + displayName + "' failed : Different options amount");
                return;
            }
        }

        List<OptionType> types = new ArrayList<>();
        typesNames.forEach(type -> {
            try{
                types.add(OptionType.valueOf(type));
            }catch(IllegalArgumentException e){
                Log.error("Loading slash command '" + displayName + "' : Failed to load parameter type '" + type + "' :  Type does not exist");
            }
        });

        this.parameters = new ArrayList<>();
        for(int i = 0; i < names.size(); i++)
            this.parameters.add(new Parameter(types.get(i), names.get(i), descriptions.get(i), required.get(i), autoCompletes.get(i), choice.get(i).toArray(new String[0])));

        Log.info("Slash command '" + displayName + "' loaded successfully");
    }

    /**
     * To execute the command
     * @param bot The current bot
     * @param guild The guild where the slash command is used
     * @param channel The text channel where the slash command is used
     * @param author The author of the slash command
     * @param member The author of the slash command as a member of the guild
     * @param event The event
     */
    public void execute(Bot bot, Guild guild, TextChannel channel, User author, Member member, SlashCommandInteractionEvent event) {
        executor.execute(bot, guild, channel, author, member, event);
    }

    /**
     * To get displayName
     * @return displayName
     */
    public String getDisplayName() { return displayName; }

    /**
     * To get description
     * @return Description
     */
    public String getDescription() { return description; }

    /**
     * To get permissions
     * @return Permissions
     */
    public Permission[] getPermissions() { return permissions; }

    /**
     * To get parameters
     * @return Parameters
     */
    public List<Parameter> getParameters(){ return parameters; }
}