package de.dosmike.sponge.vshop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

public class DependingSuggestionElement extends CommandElement {

    public static DependingSuggestionElement denenentSuggest(CommandElement wrapped, Text dependentKey, Function<List<String>, Iterable<String>> denendentSuggestions) {
        return new DependingSuggestionElement(wrapped, denendentSuggestions, true);
    }
    public static DependingSuggestionElement denenentSuggest(CommandElement wrapped, Text dependentKey, Function<List<String>, Iterable<String>> denendentSuggestions, boolean requireBegin) {
        return new DependingSuggestionElement(wrapped, denendentSuggestions, requireBegin);
    }

    private final CommandElement wrapped;
    private final Function<List<String>, Iterable<String>> suggestions;
    private final boolean requireBegin;

    protected DependingSuggestionElement(CommandElement wrapped, Function<List<String>, Iterable<String>> suggestions, boolean requireBegin) {
        super(wrapped.getKey());
        this.wrapped = wrapped;
        this.suggestions = suggestions;
        this.requireBegin = requireBegin;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        wrapped.parse(source, args, context);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        try {
            Method delegate = wrapped.getClass().getMethod("parseValue", CommandSource.class, CommandArgs.class);
            delegate.setAccessible(true);
            return delegate.invoke(wrapped, source, args);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        if (this.requireBegin) {
            String arg = args.nextIfPresent().orElse("");
            return ImmutableList.copyOf(Iterables.filter(this.suggestions.apply(args.getAll()), f -> f.startsWith(arg)));
        } else {
            return ImmutableList.copyOf(this.suggestions.apply(args.getAll()));
        }
    }

}
