package mx.kenzie.centurion;

import lombok.Data;
import mx.kenzie.centurion.arguments.CompoundArgument;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgumentContainer {
  protected final Argument<?>[] arguments;
  protected final Argument<?>[] literals;

  protected ArgumentContainer(Argument<?>... arguments) {
    this.arguments = arguments;
    final List<Argument<?>> list = new ArrayList<>(arguments.length);
    for (Argument<?> argument : arguments) if (argument.literal() && !argument.optional()) list.add(argument);
    this.literals = list.toArray(new Argument[0]);
  }

  public boolean requireEmpty() {
    return true;
  }

  public int weight() {
    int weight = 0;
    for (Argument<?> argument : arguments) weight += argument.weight();
    return weight;
  }

  public int size() {
    return arguments.length;
  }

  public boolean hasInput() {
    for (Argument<?> argument : arguments) if (!argument.literal()) return true;
    return false;
  }

  public boolean hasOptional() {
    for (Argument<?> argument : arguments) if (argument.optional()) return true;
    return false;
  }

  public Object[] check(String input, boolean passAllArguments) {
    final Result result = this.consume(input, passAllArguments);
    if (result == null) return null;
    return result.inputs;
  }

  public Result consume(String initial, boolean passAllArguments) {
    String input = initial;
    final Command<?>.Context context = Command.getContext();
    final List<Object> inputs = new ArrayList<>(8);
    if (context != null && context.arguments == null) context.arguments = inputs;
    for (Argument<?> argument : arguments) {
      final Argument.ParseResult result = argument.read(input);
      if (result == null) return null;
      final String part = result.part();
      input = result.remainder();
      if (part.isEmpty() && argument.optional()) {
        inputs.add(argument.lapse());
        continue;
      }
      if (!argument.matches(part)) return null;
      if (!passAllArguments && argument.literal()) continue;
      inputs.add(argument.parse(part));
    }
    if (this.requireEmpty() && !StringUtils.isBlank(input)) return null;
    final String part = initial.substring(0, initial.length() - input.length()).trim();
    final String remainder = StringUtils.stripStart(input, null);
    return new Result(part, remainder, inputs.toArray(new Object[0]));
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (Argument<?> argument : arguments) {
      builder.append(' ');
      final boolean optional = argument.optional(), literal = argument.literal(), plural = argument.plural();
      if (optional) builder.append('[');
      else if (!literal) builder.append('<');
      if (argument instanceof CompoundArgument<?>) builder.append('*');
      builder.append(argument.label());
      if (plural) builder.append("...");
      if (optional) builder.append(']');
      else if (!literal) builder.append('>');
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArgumentContainer)) return false;
    return Arrays.equals(arguments, ((ArgumentContainer) o).arguments);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(arguments);
  }

  public Argument<?>[] arguments() {
    return arguments;
  }

  @Data
  public class Result {
    private final String part;
    private final String remainder;
    private final Object[] inputs;
  }

}
