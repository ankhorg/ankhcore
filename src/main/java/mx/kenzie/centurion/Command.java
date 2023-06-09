package mx.kenzie.centurion;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import mx.kenzie.centurion.arguments.LiteralArgument;
import org.inksnow.ankh.core.api.util.DcLazy;

import java.util.*;
import java.util.function.BiFunction;

public abstract class Command<Sender> implements Described {

  public static final Input<?> BEHAVIOR_DEFAULT_LAPSE = (sender, arguments) -> CommandResult.NO_BEHAVIOUR;
  private static final ThreadLocal<Object> context = new ThreadLocal<>();
  protected String description;
  private DcLazy<Behaviour> behaviour;

  protected Command() {
    this.behaviour = DcLazy.of(this::create);
  }

  public static List<Argument<?>> coerce(Collection<Object> arguments) {
    final List<Argument<?>> list = new ArrayList<>(arguments.size());
    for (Object argument : arguments) {
      if (argument instanceof String) list.add(new LiteralArgument((String) argument));
      else if (argument instanceof Argument<?>) list.add((Argument<?>) argument);
      else if (argument == Boolean.class) list.add(Arguments.BOOLEAN);
      else if (argument == String.class) list.add(Arguments.STRING);
      else throw new RuntimeException("Unknown argument acceptor provided. " + argument);
    }
    return list;
  }

  /**
   * Command context is available during the parsing and execution phase, and discarded once complete.
   * This is a thread-safe value and a strong reference.
   */
  @SuppressWarnings("unchecked")
  public static <Sender> Command<Sender>.Context getContext() {
    return (Command<Sender>.Context) context.get();
  }

  protected static void setContext(Command<?>.Context context) {
    Command.context.set(context);
  }

  public Behaviour behaviour() {
    return behaviour.get();
  }

  public abstract Behaviour create();

  protected Behaviour command(String label, String... aliases) {
    return new Behaviour(label, aliases);
  }

  public Result execute(Sender sender, String input) {
    return behaviour().execute(sender, input);
  }

  public String[] patterns() {
    return behaviour().patterns();
  }

  @Override
  public String description() {
    return description;
  }

  public String label() {
    return behaviour().label;
  }

  public Set<String> aliases() {
    return new HashSet<>(behaviour().aliases);
  }

  public Collection<ArgumentContainer> arguments() {
    this.behaviour().sort();
    return new ArrayList<>(behaviour().arguments);
  }

  @FunctionalInterface
  public interface Input<Sender> extends BiFunction<Sender, Arguments, Result> {

    Result run(Sender sender, Arguments arguments) throws Throwable;

    @Override
    default Result apply(Sender sender, Arguments arguments) {
      try {
        return this.run(sender, arguments);
      } catch (Throwable ex) {
        return new Result.Error(CommandResult.FAILED_EXCEPTION, ex);
      }
    }

  }

  @FunctionalInterface
  public interface EmptyInput<Sender> extends Input<Sender>, BiFunction<Sender, Arguments, Result> {

    Result run(Sender sender) throws Throwable;

    @Override
    default Result run(Sender sender, Arguments arguments) throws Throwable {
      return this.run(sender);
    }

  }

  @SuppressWarnings("unchecked")
  public class Behaviour {
    @Getter
    private final String label;
    @Getter
    private final Set<String> aliases;
    @Getter
    private final Map<ArgumentContainer, Input<Sender>> functions;
    @Getter
    private final List<ArgumentContainer> arguments;
    @Getter
    @Setter
    private Input<Sender> lapse = (Input<Sender>) BEHAVIOR_DEFAULT_LAPSE;
    @Getter
    private boolean sorted;
    @Getter
    private String[] patterns;
    @Getter
    @Setter
    private boolean passAllArguments;

    protected Behaviour(String label, String... aliases) {
      this.label = label.toLowerCase();
      this.aliases = new HashSet<>(Arrays.asList(aliases));
      this.arguments = new LinkedList<>();
      this.functions = new LinkedHashMap<>();
    }

    private String prepareArguments(String input) {
      this.sort();
      remove_name:
      {
        if (input.startsWith(label)) {
          if (input.length() == label.length()) {
            input = "";
            break remove_name;
          }
          input = input.substring(label.length() + 1);
          break remove_name;
        }
        for (String alias : aliases)
          if (input.startsWith(alias)) {
            if (input.length() == alias.length()) {
              input = "";
              break remove_name;
            }
            input = input.substring(alias.length() + 1);
            break remove_name;
          }
      }
      input = input.trim();
      return input;
    }

    public Behaviour arg(Object arg1, Input<Sender> function) {
      return this.arg(Lists.newArrayList(arg1), function);
    }

    public Behaviour arg(Object arg1, Object arg2, Input<Sender> function) {
      return this.arg(Lists.newArrayList(arg1, arg2), function);
    }

    public Behaviour arg(Object arg1, Object arg2, Object arg3, Input<Sender> function) {
      return this.arg(Lists.newArrayList(arg1, arg2, arg3), function);
    }

    public Behaviour arg(Object arg1, Object arg2, Object arg3, Object arg4, Input<Sender> function) {
      return this.arg(Lists.newArrayList(arg1, arg2, arg3, arg4), function);
    }

    public Behaviour arg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Input<Sender> function) {
      return this.arg(Lists.newArrayList(arg1, arg2, arg3, arg4, arg5), function);
    }

    public Behaviour arg(Collection<Object> arguments, Input<Sender> function) {
      final List<Argument<?>> list = coerce(arguments);
      final ArgumentContainer container = new ArgumentContainer(list.toArray(new Argument[0]));
      this.arguments.add(container);
      this.functions.put(container, function);
      this.sorted = false;
      return this;
    }

    protected void sort() {
      if (sorted) return;
      this.sorted = true;
      this.arguments.sort(Comparator.comparing(ArgumentContainer::weight));
      int index = 0;
      if (lapse != BEHAVIOR_DEFAULT_LAPSE) {
        this.patterns = new String[arguments.size() + 1];
        this.patterns[0] = label;
        for (ArgumentContainer argument : arguments) patterns[++index] = label + argument.toString();
      } else {
        this.patterns = new String[arguments.size()];
        for (ArgumentContainer argument : arguments) patterns[index++] = label + argument.toString();
      }
    }

    @Override
    public String toString() {
      return "Behaviour{" +
          "label='" + label + '\'' +
          ", aliases=" + aliases +
          ", functions=" + functions +
          ", arguments=" + arguments +
          ", lapse=" + lapse +
          ", sorted=" + sorted +
          ", patterns=" + Arrays.toString(patterns) +
          '}';
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(label, aliases, functions, arguments, lapse, sorted);
      result = 31 * result + Arrays.hashCode(patterns);
      return result;
    }

    public Result execute(Sender sender, String input) {
      Command.setContext(new Context(sender, input));
      try {
        input = this.prepareArguments(input);
        if (input.isEmpty()) return lapse.apply(sender, new Arguments());
        final String lower = input.toLowerCase();
        loop:
        for (ArgumentContainer argument : arguments) {
          for (Argument<?> literal : argument.literals)
            if (!lower.contains(literal.label().toLowerCase())) continue loop;
          final Object[] inputs = argument.check(input, passAllArguments);
          if (inputs == null) continue;
          final Input<Sender> function = functions.get(argument);
          assert function != null;
          final Result result = function.apply(sender, new Arguments(argument, inputs));
          if (result.type().endParsing) return result;
          if (result == CommandResult.LAPSE) break;
        }
        return lapse.apply(sender, new Arguments());
      } finally {
        Command.setContext(null);
      }
    }
  }

  public class Context {

    protected final Sender sender;
    protected final String rawInput;
    protected List<Object> arguments;

    public Context(Sender sender, String input) {
      this.sender = sender;
      this.rawInput = input;
    }

    public Sender getSender() {
      return sender;
    }

    public String getRawInput() {
      return rawInput;
    }

    public Command<Sender> getCommand() {
      return Command.this;
    }

    public List<Object> getArguments() {
      return arguments;
    }

  }

}
