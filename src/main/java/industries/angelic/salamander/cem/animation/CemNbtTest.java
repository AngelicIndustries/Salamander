package industries.angelic.salamander.cem.animation;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One NBT test as ETF reads it, for random entity rules ({@code nbt.1.<path>=<test>}) and EMF's {@code nbt()}.
 * Syntax: {@code [print:][!]} then {@code raw:<match>}, {@code exists:true|false}, {@code range:<a>[-<b>]} or a
 * plain {@code <match>}. A match is {@code regex:}, {@code iregex:}, {@code pattern:}, {@code ipattern:} (globs with
 * {@code *} and {@code ?}), or values separated by spaces, any of which may equal the value; each may start with
 * {@code !}. Raw matches test a value's SNBT; the others its plain text. A missing value passes only
 * {@code exists:false} and a blank {@code raw:}.
 */
public final class CemNbtTest {

    /** The values found at an NBT path, plain or as SNBT; empty when the path is missing. */
    public interface Source {

        List<String> nbt(String path, boolean raw);
    }

    private static final Pattern QUOTED = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
    private static final Pattern RANGE = Pattern.compile("(-?\\d+)(?:-(-?\\d+))?");

    private final boolean invert;
    private final boolean raw;
    private final boolean passesWhenMissing;
    private final Predicate<String> test;

    private CemNbtTest(boolean invert, boolean raw, boolean passesWhenMissing, Predicate<String> test) {
        this.invert = invert;
        this.raw = raw;
        this.passesWhenMissing = passesWhenMissing;
        this.test = test;
    }

    public static CemNbtTest of(String instruction) {
        if (instruction == null || instruction.trim()
            .isEmpty()) throw new IllegalArgumentException("Blank NBT test");
        String value = instruction.trim()
            .replace("print_raw:", "print:raw:")
            .replaceFirst("^print_all:", "");
        if (value.startsWith("print:")) value = value.substring(6);
        boolean invert = value.startsWith("!");
        if (invert) value = value.substring(1);
        if (value.startsWith("raw:")) {
            String match = value.substring(4);
            boolean blank = match.trim()
                .isEmpty();
            return new CemNbtTest(
                invert,
                true,
                blank,
                blank ? s -> s.trim()
                    .isEmpty() : matcher(match));
        }
        if (value.startsWith("exists:")) {
            boolean exists = value.contains("exists:true");
            return new CemNbtTest(invert, false, value.contains("exists:false"), s -> exists);
        }
        if (value.startsWith("range:")) {
            Matcher range = RANGE.matcher(
                value.substring(6)
                    .trim());
            if (!range.matches()) throw new IllegalArgumentException("Invalid NBT range: " + instruction);
            long min = Long.parseLong(range.group(1));
            long max = range.group(2) == null ? min : Long.parseLong(range.group(2));
            return new CemNbtTest(invert, false, false, s -> {
                try {
                    long number = (long) Double.parseDouble(s);
                    return number >= Math.min(min, max) && number <= Math.max(min, max);
                } catch (NumberFormatException e) {
                    return false;
                }
            });
        }
        return new CemNbtTest(invert, false, false, matcher(value));
    }

    public boolean test(Source source, String path) {
        List<String> values = source.nbt(path, raw);
        boolean passes = passesWhenMissing;
        if (!values.isEmpty()) {
            passes = false;
            for (String value : values) {
                if (value != null && test.test(value)) {
                    passes = true;
                    break;
                }
            }
        }
        return passes != invert;
    }

    /** A regex, glob or value list match, as ETF matches strings. */
    public static Predicate<String> matcher(String line) {
        String value = line.trim();
        boolean invert = value.startsWith("!");
        if (invert) value = value.substring(1);
        Predicate<String> positive;
        if (value.startsWith("regex:") || value.startsWith("iregex:")) {
            boolean ignoreCase = value.startsWith("i");
            Pattern regex = Pattern.compile((ignoreCase ? "(?i)" : "") + value.substring(value.indexOf(':') + 1));
            positive = s -> regex.matcher(s)
                .matches();
        } else if (value.startsWith("pattern:") || value.startsWith("ipattern:")) {
            boolean ignoreCase = value.startsWith("i");
            StringBuilder glob = new StringBuilder(ignoreCase ? "(?i)" : "");
            for (char c : value.substring(value.indexOf(':') + 1)
                .toCharArray()) glob.append(c == '*' ? ".*" : c == '?' ? "." : Pattern.quote(String.valueOf(c)));
            Pattern regex = Pattern.compile(glob.toString());
            positive = s -> regex.matcher(s)
                .matches();
        } else {
            String whole = value;
            List<String> tokens = Arrays.asList(whole.split("\\s+"));
            boolean quoted = whole.contains("\"");
            positive = s -> {
                if (s.equals(whole) || tokens.contains(s)) return true;
                if (quoted) {
                    Matcher groups = QUOTED.matcher(whole);
                    while (groups.find()) if (s.equals(
                        groups.group(1)
                            .replace("\"", "")
                            .trim()))
                        return true;
                }
                return false;
            };
        }
        return s -> positive.test(s) != invert;
    }
}
