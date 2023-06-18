package fr.dtn.jdl.slashcommand;

import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Class that represents a parameter of a slash command
 */
public class Parameter{
    /**
     * The type of the argument
     */
    private final OptionType type;
    /**
     * The name and the description of the parameter
     */
    private final String name, description;
    /**
     * If the parameter is required and if auto complete is enabled on it
     */
    private final boolean required, autoComplete;
    /**
     * The different choices that can be entered on the parameter
     */
    private final String[] choices;

    /**
     * Constructor
     * @param type The type of the argument
     * @param name The name of the parameter
     * @param description The description of the parameter
     * @param required If the parameter is required or not
     * @param autoComplete If the auto complete is enabled on the parameter
     * @param choices The different choices that can be entered on the parameter
     */
    public Parameter(OptionType type, String name, String description, boolean required, boolean autoComplete, String[] choices){
        this.type = type;
        this.name = name;
        this.description = description;
        this.required = required;
        this.autoComplete = autoComplete;
        this.choices = choices;
    }

    /**
     * To get type
     * @return Type
     */
    public OptionType getType() { return type; }

    /**
     * To get name
     * @return Name
     */
    public String getName() { return name; }

    /**
     * To get description
     * @return Description
     */
    public String getDescription() { return description; }

    /**
     * To get required
     * @return Required
     */
    public boolean isRequired() { return required; }

    /**
     * To get autoComplete
     * @return autoComplete
     */
    public boolean isAutoComplete() { return autoComplete; }

    /**
     * To get choices
     * @return Choices
     */
    public String[] getChoices() { return choices; }
}