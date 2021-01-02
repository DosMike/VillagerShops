package de.dosmike.sponge.vshop;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DependingSuggestionElement extends CommandElement {

	private final CommandElement wrapped;
	private final Function<List<String>, Iterable<String>> suggestions;
	private final boolean requireBegin;
	protected DependingSuggestionElement(CommandElement wrapped, Function<List<String>, Iterable<String>> suggestions, boolean requireBegin) {
		super(wrapped.getKey());
		this.wrapped = wrapped;
		this.suggestions = suggestions;
		this.requireBegin = requireBegin;
	}

	public static DependingSuggestionElement dependentSuggest(CommandElement wrapped, Function<List<String>, Iterable<String>> dependentSuggestions) {
		return new DependingSuggestionElement(wrapped, dependentSuggestions, true);
	}

	public static DependingSuggestionElement dependentSuggest(CommandElement wrapped, Function<List<String>, Iterable<String>> dependentSuggestions, boolean requireBegin) {
		return new DependingSuggestionElement(wrapped, dependentSuggestions, requireBegin);
	}

	@Override
	public void parse(@NotNull CommandSource source, @NotNull CommandArgs args, @NotNull CommandContext context) throws ArgumentParseException {
		wrapped.parse(source, args, context);
	}

	@Nullable
	@Override
	protected Object parseValue(@NotNull CommandSource source, @NotNull CommandArgs args) {
		try {
			Method delegate = wrapped.getClass().getMethod("parseValue", CommandSource.class, CommandArgs.class);
			delegate.setAccessible(true);
			return delegate.invoke(wrapped, source, args);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	@Override
	public List<String> complete(@NotNull CommandSource src, @NotNull CommandArgs args, @NotNull CommandContext context) {
		if (this.requireBegin) {
			String arg = args.nextIfPresent().orElse("");
			return ImmutableList.copyOf(StreamSupport.stream(this.suggestions.apply(args.getAll()).spliterator(), false).filter(f -> f.startsWith(arg)).collect(Collectors.toList()));
		} else {
			return ImmutableList.copyOf(this.suggestions.apply(args.getAll()));
		}
	}

}
