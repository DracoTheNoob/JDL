package fr.dtn.jdl;

import com.moandjiezana.toml.Toml;
import fr.dtn.jdl.command.Command;
import fr.dtn.jdl.event.EventHandler;
import fr.dtn.jdl.slashcommand.Parameter;
import fr.dtn.jdl.slashcommand.SlashCommand;
import fr.dtn.jll.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Class that represents a Discord bot
 */
public class Bot {
    /**
     * Contains all the embeds that are prebuilt and used for generic messages such as NO_PERMISSION one
     */
    static final HashMap<DefaultEmbed, MessageEmbed> embeds;

    /**
     * Bot itself
     */
    private final ShardManager bot;

    /**
     * Contains all the event handlers
     */
    private final List<EventHandler<?>> eventHandlers;

    /**
     * Contains all the raw text commands
     */
    private final HashMap<String, Command> commands;

    /**
     * Contains all the slash commands
     */
    private final HashMap<String, SlashCommand> slashCommands;

    /**
     * The prefix before each raw text command
     */
    private final String prefix;

    /**
     * Utility method to load activity from configuration file string
     * @param text The configuration of the activity field in configuration file
     * @return The corresponding activity
     */
    Activity getActivity(String text){
        return Activity.of(
                Activity.ActivityType.valueOf(text.split(" ")[0]),
                text.substring(text.split(" ")[0].length() + 1)
        );
    }

    // To load all the default embeds
    static{
        embeds = new HashMap<>();

        EmbedBuilder noPermission = new EmbedBuilder()
                .setColor(Color.red)
                .setTitle("[ERROR]")
                .addField("Permission refused", "You cannot use this command because of missing permission(s).", true);

        EmbedBuilder slashCommandMissingExecutor = new EmbedBuilder()
                .setColor(Color.red)
                .setTitle("[ERROR]")
                .addField("No executor", "I'm sorry, but it seems that this command is not handled by the bot, please report this to bot developer.", true);

        embeds.put(DefaultEmbed.NO_PERMISSION, noPermission.build());
        embeds.put(DefaultEmbed.SLASH_COMMAND_MISSING_EXECUTOR, slashCommandMissingExecutor.build());
    }

    /**
     * Constructor
     * @param directory The directory which the bot information are stored in
     */
    public Bot(File directory) {
        this.eventHandlers = new ArrayList<>();
        this.commands = new HashMap<>();
        this.slashCommands = new HashMap<>();

        Log.setDirectory(new File(directory, "logs"));
        Log.info("Instantiating bot on directory : '" + directory.getPath() + "'");

        File config = new File(directory, "configuration.toml");
        Log.info("Loading configuration from : '" + config.getPath() + "'");
        Toml configuration;
        try{
            configuration = new Toml().read(config);
        }catch(Exception e){
            try{
                URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("default_configuration.toml")).toURI();
                File defaultConfig = new File(uri);

                FileReader read = new FileReader(defaultConfig);
                BufferedReader reader = new BufferedReader(read);

                StringBuilder raw = new StringBuilder();
                reader.lines().forEach(line -> raw.append(line).append('\n'));

                reader.close();
                read.close();

                FileWriter write = new FileWriter(config);
                BufferedWriter writer = new BufferedWriter(write);

                writer.write(raw.toString());

                writer.close();
                write.close();
            }catch(IOException e1){
                e1.printStackTrace();
            }catch(URISyntaxException e1){
                throw new RuntimeException(e1);
            }

            throw new RuntimeException("Failed to load '" + config.getPath() + "' configuration file : creating default one");
        }

        Log.info("Creating Discord Bot");
        List<GatewayIntent> intents = new ArrayList<>();

        for(Object o : configuration.getList("bot.intents")) {
            String intent = o.toString().toUpperCase();

            try {
                intents.add(GatewayIntent.valueOf(intent));
            }catch(IllegalArgumentException e){
                Log.error("Unable to add enable intent '" + intent + "' : Intent does not exist");
            }
        }

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(configuration.getString("bot.token"), intents);

        if(configuration.getString("bot.status") == null || configuration.getString("bot.status").equals("")) {
            Log.warn("Bot status not set in 'configuration.toml', loading default 'ONLINE' status");
            builder.setStatus(OnlineStatus.ONLINE);
        }else{
            OnlineStatus status = OnlineStatus.valueOf(configuration.getString("bot.status"));
            Log.info("Set status to " + status);
            builder.setStatus(status);
        }

        if(configuration.getString("bot.activity") == null || configuration.getString("bot.activity").equals("")){
            Log.warn("Bot activity not set in 'configuration.toml', loading default empty activity");
            builder.setActivity(null);
        }else{
            Activity activity = getActivity(configuration.getString("bot.activity"));
            Log.info("Loading activity " + activity);
            builder.setActivity(activity);
        }

        this.bot = builder.build();

        Log.info("Adding events listener");
        this.bot.addEventListener(new EventsListener(this));

        Log.info("Created bot successfully");
        this.prefix = configuration.getString("bot.prefix");
    }

    /**
     * To add an EventHandler
     * @param handler The EventHandler to add
     */
    public void registerEventHandler(EventHandler<?> handler){ this.eventHandlers.add(handler); }

    /**
     * To add a raw text command from its configuration file
     * @param file The raw text command configuration file
     */
    public void registerCommand(File file){
        Command command = new Command(file);

        for(String call : command.getCalls())
            this.commands.put(call, command);
    }

    /**
     * To register all the commands of a directory and possibly its children directories
     * @param directory The root directory of the raw text commands to load
     * @param loadSubFiles If the method considers or not the files stored in children directories of the root one
     */
    public void registerCommands(File directory, boolean loadSubFiles){
        if(directory == null || !directory.isDirectory() || !directory.exists() || directory.listFiles() == null)
            return;

        for(File file : directory.listFiles()){
            if(file.isDirectory() && loadSubFiles)
                registerCommands(file, true);
            else
                registerCommand(file);
        }
    }

    /**
     * To register a slash command from its configuration file
     * @param file The slash command configuration file
     */
    public void registerSlashCommand(File file){
        SlashCommand command = new SlashCommand(file);
        SlashCommandData data = Commands.slash(command.getDisplayName(), command.getDescription());

        for(Parameter parameter : command.getParameters()) {
            OptionData option = new OptionData(parameter.getType(), parameter.getName(), parameter.getDescription());
            String[] choices = parameter.getChoices();

            for(String choice : choices)
                option.addChoices(new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice));

            data.addOptions(option);
        }

        this.bot.getShards().forEach(jda -> jda.updateCommands().addCommands(data).queue());
        this.slashCommands.put(command.getDisplayName(), command);
    }

    /**
     * To load all the slash commands from a directory and possibly its children directories
     * @param directory The root directory of the slash commands configuration files
     * @param loadSubFiles If the method considers or not the files stored in children directories of the root one
     */
    public void registerSlashCommands(File directory, boolean loadSubFiles){
        if(directory == null || !directory.isDirectory() || !directory.exists() || directory.listFiles() == null)
            return;

        for(File file : directory.listFiles()){
            if(file.isDirectory() && loadSubFiles)
                registerSlashCommands(file, true);
            else
                registerSlashCommand(file);
        }
    }

    /**
     * Different types of default library messages embeds
     */
    enum DefaultEmbed {
        NO_PERMISSION, // If a member does not have the permission to execute an action
        SLASH_COMMAND_MISSING_EXECUTOR; // If a registered slash command does not have any executor
    }

    /**
     * Class managing event listening
     */
    static class EventsListener extends ListenerAdapter {
        /**
         * The current bot instance
         */
        private final Bot bot;

        /**
         * Constructor
         * @param bot The current bot instance
         */
        public EventsListener(Bot bot){
            this.bot = bot;
        }

        /**
         * To execute all the event handlers that corresponds to the current happening event
         * @param event The event that happened
         */
        @Override
        public void onGenericEvent(GenericEvent event) {
            String eventId = event.getClass().getSimpleName().toLowerCase().replace("event", "");
            Log.info("Event : " + eventId);

            for(EventHandler<?> handler : bot.eventHandlers){
                try{
                    for(Type type : handler.getClass().getGenericInterfaces()) {
                        try {
                            for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
                                Class<? extends GenericEvent> caster = (Class<? extends GenericEvent>) arg;

                                if (caster.isInstance(event)) {
                                    happen(handler.getClass().cast(handler), caster.cast(event));
                                    break;
                                }
                            }
                        }catch(ClassCastException ignored){}
                    }
                }catch(Exception ignored){}
            }
        }

        // Utility method
        <T extends GenericEvent> void happen(EventHandler<T> handler, T event){ handler.happen(bot, event); }

        /**
         * To send a message in the console when the bot is ready
         * @param event The event
         */
        @Override
        public void onReady(@NotNull ReadyEvent event){
            Log.info("Bot is ready");
        }

        /**
         * To manage raw text commands
         * @param event Event that happen when a message is sent
         */
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            if(!event.getMessage().getContentRaw().startsWith(bot.prefix) || event.getAuthor().isBot())
                return;

            Guild guild = event.getGuild();
            TextChannel channel = event.getChannel().asTextChannel();
            Message message = event.getMessage();
            User user = event.getAuthor();
            Member member = event.getMember();

            if(member == null)
                return;

            String raw = message.getContentRaw();
            while(raw.contains("  ")) raw = raw.replace("  ", " ");

            String[] split = raw.split(" ");
            String call = split[0].substring(bot.prefix.length());
            String[] args = new String[split.length - 1];
            System.arraycopy(split, 1, args, 0, args.length);

            Command command = bot.commands.getOrDefault(call, null);

            if(command == null)
                return;

            if(!member.hasPermission(command.getPermissions())){
                Log.info("@"+user.getName()+" (" + member.getNickname()+") tried to call command '"+command.getDisplayName()+"' (" + member.getNickname()+") on ("+guild.getName()+"#"+channel.getName()+") -> refused : missing permission(s)");
                message.replyEmbeds(embeds.get(DefaultEmbed.NO_PERMISSION)).queue();
                return;
            }

            Log.info("'"+command.getDisplayName()+"' called by @"+user.getName()+" (" + member.getNickname()+") on ("+guild.getName()+"#"+channel.getName()+")");
            command.execute(bot, guild, channel, message, user, member, args);
        }

        /**
         * To manage slash commands
         * @param event Event that happen when a slash command is used
         */
        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            Guild guild = event.getGuild();

            if(guild == null)
                return;

            TextChannel channel = event.getChannel().asTextChannel();
            User author = event.getUser();
            Member member = event.getMember();

            if(member == null)
                return;

            SlashCommand command = bot.slashCommands.getOrDefault(event.getName(), null);

            if(command == null){
                Log.warn("No executor for slash command '" + event.getName() + "' : Unable to execute it");
                event.deferReply().queue();
                event.getHook().setEphemeral(true).sendMessageEmbeds(embeds.get(DefaultEmbed.SLASH_COMMAND_MISSING_EXECUTOR)).queue();
                return;
            }

            if(!member.hasPermission(command.getPermissions())){
                Log.info("@"+author.getName()+" (" + member.getNickname()+") tried to call slash command '"+command.getDisplayName()+"' (" + member.getNickname()+") on ("+guild.getName()+"#"+channel.getName()+") -> refused : missing permission(s)");
                event.deferReply().queue();
                event.getHook().setEphemeral(true).sendMessageEmbeds(embeds.get(DefaultEmbed.NO_PERMISSION)).queue();
                return;
            }

            Log.info(author.getName()+" ("+member.getNickname()+") use slash command '"+command.getDisplayName()+"' in ("+guild.getName()+"/"+channel.getName()+")");
            command.execute(bot, guild, channel, author, member, event);
        }
    }
}