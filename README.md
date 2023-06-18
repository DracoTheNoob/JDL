# JDL #
JDL helps making discord bot. It is based on JDA, but simplify many uses of it.

## Dependencies ##
The project is first using JDA("net.dv8tion:JDA:5.0.0-beta.10"), and a Moandjiezana's Toml parser("com.moandjiezana.toml:toml4j:0.7.2").
The project also use JLL([Github Repo](https://github.com/DracoTheNoob/JLL)), a logging library I made, so consider check the lib repo to be able to use this lib (don't worry, it is really basic).

## Create a bot ##
Once you created the bot in Discord Dev section, and invited it to your debugging server, you will need to use the following code to start using the JDL :

```java
File directory = new File(/* your directory path */);
Bot bot = new Bot(directory);
```
The directory will be the one that store all the bot and libary files, like configuration files or commands ones (we will call this directory the bot directory). If you run the code without a 'configuration.toml' file into your bot directory, the project will throw a Runtime Exception and create one from a template. You can fill it by following this guide : 

```toml
[bot]
token = ''
status = ''
activity = ''
prefix = ''
intents = []
```
- The token field correspond to the token of your bot.
- The status field correspond to the Online Status of your bot, it can only be one of the following values : [ ONLINE / IDLE / DO_NOT_DISTURB / INVISIBLE / OFFLINE ].
- The activity field correspond to the Activity of your bot, it needs to start with the activity type, which can only be one of the following values : [ PLAYING / STREAMING / LISTENING / CUSTOM_STATUS / COMPETING ]. The second part, separated with a space from the activity type, is the text content of the activity. Example : 'PLAYING github commits'.
- The prefix field correspond to the prefix that is used to write raw text commands. I call a raw text command all commands that are executed by writing a prefix and a command name, those commands are different of the slash commands created by Discord.
- The intents array field corresponds to all the intents that will be enabled on your bot.

After the configuration is done, you would be able to start the program and see your bot connected on your Discord application. If there is an exception on your console, consider remaking the previous steps.

## Event handling ##

To handle events, you first need to create an implementation of the EventHandler interface, here is an example for the ReadyEvent :
```java
public class ReadyHandler implements EventHandler<ReadyEvent> {
    @Override
    public void happen(Bot bot, ReadyEvent event) {
        Log.info("Bot is awesomely ready !");
    }
}
```
The generic type in the EventHandler implementation is the type of event that will be handled.
To register our new EventHandler, we need to re-use our previous code :
```java
File directory = new File(/* your directory path */);
Bot bot = new Bot(directory);
```
But we will add a new line to register the EventHandler :
```java
bot.registerEventHandler(new ReadyHandler());
```
With this code, we now send a message in console when the bot is ready (it is only an example, because in case, the lib already do that).

## Raw text commands ##

### Warning ###
Before creating raw text commands, you need to first know that this system requires two intents : 'GUILD_MESSAGES' and 'MESSAGE_CONTENT'. If one of them is missing, the raw text command system will not work.

### Implementation ###
To add raw text commands, we will first create our command, by firstly create the command file, corresponding to the following one :
```toml
description = ''
calls = []
permissions = []
executor = ''
```
It is important to have a Toml file for the command file. The name of the file will be the command display name.
- The description field corresponds to the description of the command.
- The calls array field corresponds to all the strings that are used to call the command.
- The permissions array field corresponds to all the premissions required to execute the command.
- The executor field corresponds to the path to the command executor class.

With an example for a ping command, stored in 'ping.toml' file, inside 'commands' directory, itself inside our bot directory : 
```toml
description = 'A command to test the bot'
calls = [ 'ping', 'p' ]
permissions = []
executor = 'fr.test.CommandPing'
```

We can now add our command class :
```java
package fr.test;

public class CommandPing implements CommandExecutor {
    @Override
    public void execute(Bot bot, Guild guild, TextChannel channel, Message message, User author, Member member, String[] args) {
        message.reply("pong").queue();
    }
}
```
This command only responds "pong" when the command ping is called.

And finally, we can register the command to the bot :
```java
bot.registerCommand(new File(directory, "commands/ping.toml"));
```
On this example, the file path is "directory/commands/ping.toml" where "directory" is the path of the bot directory. I advise to store all your commands into the same directory, or in subfolders of the same directory, because with the following method, you can automatically load all the commands of a directory, including or not sub-directories :
```java
bot.registerCommands(new File(directory, "commands"), true);
```

## Slash commands ##
Slash commands are more complex to implement than raw text commands, but those first are more flexible and powerful. First, let's create the our slash command file from the following template :
```toml
description = ''
permissions = []
executor = ''

[parameters]
name = []
description = []
required = []
autoComplete = []
choice = []
type = []
```
- Description, permissions and executor field corresponds to the same as raw text commands.
- Parameters.name array field corresponds to the names of the different parameters.
- Parameters.description array field corresponds to the descriptions of the different parameters.
- Parameters.required array field corresponds to if the corresponding parameter is mandatory or not.
- Parameters.autoComplete array field corresponds to if the corresponding parameter is auto completed or not.
- Parameters.choice array field corresponds to the differents choices per parameters.
- Parameters.type array field corresponds to the different types of parameters.

With an example for a command 'cook' that would have 2 arguments, an ingredient and a container :
```toml
description = 'Cook a dish by putting an ingredient into a container.'
permissions = []
executor = 'fr.test.cook.CommandCook'

[parameters]
name = [ 'ingredient', 'container' ]
description = [ 'The ingredient to cook.', 'The container to cook the ingredient in.' ]
required = [ true, true ]
autoComplete = [ true, true ]
choice = [ ['apple','chocolate','rice'], ['bowl','plate'] ]
type = [ 'STRING', 'STRING' ]
```

Let's now add the SlashCommandExecutor implementation as a new class :
```java
package fr.test.cook;

public class CommandCook implements SlashCommandExecutor {
    @Override
    public void execute(Bot bot, Guild guild, TextChannel channel, User user, Member member, SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String ingredient = event.getOption("ingredient").getAsString();
        String container = event.getOption("container").getAsString();
        String message = "You mixed " + ingredient + " with " + container;
        event.getHook().setEphemeral(true).sendMessage(message).queue();
    }
}
```

This is only an example code, normally, I should verify if options are not null.
Now that we have our class, let's register our slash command file :

```java
bot.registerSlashCommand(new File(directory, "slash_commands/cook.toml"));
```
Here, my slash command file is stored at 'directory/slash_commands/cook.toml', where 'directory' corresponds to the bot directory.


## Message ##
More features are coming...