package industries.angelic.salamander.cem.loading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ordered Random Entities predicates shared by model and texture selection. */
public final class CemRules {

    public interface Facts {

        String value(String key);

        default List<String> values(String key) {
            String value = value(key);
            return value == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(value);
        }

        List<String> nbt(String path, boolean raw);

        long seed();
    }

    public static final class Selection {

        public final int model, rule;

        public Selection(int model, int rule) {
            this.model = model;
            this.rule = rule;
        }
    }

    private static final Pattern RANGE = Pattern
        .compile("\\(?(-?\\d+(?:\\.\\d+)?)\\)?(?:-\\(?(-?\\d+(?:\\.\\d+)?)\\)?)?");
    private static final Map<String, String> BLOCK_ALIASES = new HashMap<>();
    private static final Set<String> EFR_PLANTS = new HashSet<>(
        Arrays.asList("cornflower", "lily_of_the_valley", "wither_rose", "beetroots", "sweet_berry_bush"));
    static {
        String[] flowers = { "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
            "white_tulip", "pink_tulip", "oxeye_daisy" };
        for (int i = 0; i < flowers.length; i++) BLOCK_ALIASES.put(flowers[i], "red_flower:" + i);
        BLOCK_ALIASES.put("dandelion", "yellow_flower:0");
        BLOCK_ALIASES.put("sunflower", "double_plant:0");
        BLOCK_ALIASES.put("lilac", "double_plant:1");
        BLOCK_ALIASES.put("rose_bush", "double_plant:4");
        BLOCK_ALIASES.put("peony", "double_plant:5");
        BLOCK_ALIASES.put("grass_block", "grass");
        BLOCK_ALIASES.put("podzol", "dirt:2");
    }
    private final List<Rule> rules = new ArrayList<>();

    public CemRules(Properties properties) {
        this(properties, false);
    }

    public CemRules(Properties properties, boolean textures) {
        SortedSet<Integer> indices = new TreeSet<>();
        String keys = textures ? "(?:textures|skins)" : "models";
        for (String key : properties.stringPropertyNames()) if (key.matches(keys + "\\.[1-9][0-9]{0,4}"))
            indices.add(Integer.parseInt(key.substring(key.indexOf('.') + 1)));
        for (int index : indices) {
            String indicesValue = textures
                ? properties.getProperty("textures." + index, properties.getProperty("skins." + index))
                : properties.getProperty("models." + index);
            Rule rule = new Rule(index, integers(indicesValue));
            String weights = properties.getProperty("weights." + index);
            if (weights != null) {
                rule.weights = integers(weights);
                if (rule.weights.length != rule.models.length) {
                    if (!textures)
                        throw new IllegalArgumentException("weights." + index + ": count differs from models");
                    int oldLength = rule.weights.length;
                    long sum = 0;
                    for (int weight : rule.weights) sum += weight;
                    rule.weights = Arrays.copyOf(rule.weights, rule.models.length);
                    if (oldLength < rule.weights.length) Arrays.fill(
                        rule.weights,
                        oldLength,
                        rule.weights.length,
                        oldLength == 0 ? 1 : (int) (sum / oldLength));
                }
            }
            for (int weight : rule.weights) {
                if (weight < 0) throw new IllegalArgumentException("Negative CEM weight");
                rule.total += weight;
            }
            if (rule.total <= 0) throw new IllegalArgumentException("CEM weights sum to zero");
            for (String key : properties.stringPropertyNames()) {
                String suffix = "." + index;
                if (key.startsWith("nbt." + index + ".")) {
                    String path = key.substring(("nbt." + index + ".").length());
                    String pattern = properties.getProperty(key)
                        .trim();
                    boolean inverted = pattern.startsWith("!");
                    String positive = inverted ? pattern.substring(1) : pattern;
                    boolean raw = positive.startsWith("raw:");
                    Predicate<String> match = match(positive);
                    rule.conditions.add(f -> {
                        List<String> values = f.nbt(path, raw);
                        boolean result = positive.startsWith("exists:")
                            ? !values.isEmpty() == Boolean.parseBoolean(positive.substring(7))
                            : values.stream()
                                .anyMatch(match);
                        return result != inverted;
                    });
                } else if (key.endsWith(suffix)) {
                    String type = key.substring(0, key.length() - suffix.length());
                    String value = properties.getProperty(key)
                        .trim();
                    if (type.equals("models") || type.equals("textures")
                        || type.equals("skins")
                        || type.equals("weights")) continue;
                    String actual = type.equals("collarColors") ? "colors" : type;
                    if (actual.equals("distance")) actual = "distanceFromPlayer";
                    if (actual.equals("isAngry")) actual = "angry";
                    Predicate<String> condition;
                    switch (actual) {
                        case "heights":
                        case "moonPhase":
                        case "dayTime":
                        case "sizes":
                        case "maxHealth":
                        case "speed":
                        case "jumpStrength":
                        case "distanceFromPlayer":
                        case "light":
                        case "hour":
                        case "minute":
                        case "second":
                        case "monthDay":
                        case "month":
                        case "weekDay":
                        case "yearDay":
                        case "year":
                            condition = range(value);
                            break;
                        case "minHeight":
                            condition = v -> v != null && Double.parseDouble(v) >= Double.parseDouble(value);
                            break;
                        case "maxHeight":
                            condition = v -> v != null && Double.parseDouble(v) <= Double.parseDouble(value);
                            break;
                        case "health":
                            condition = range(value.replace("%", ""));
                            if (value.contains("%")) actual = "healthPercent";
                            break;
                        case "name":
                            condition = match(value);
                            break;
                        case "teams":
                        case "language":
                        case "dimension":
                            condition = stringChoices(
                                actual.equals("dimension") ? value.replace("minecraft:", "") : value);
                            break;
                        case "biomes":
                        case "colors":
                        case "weather":
                        case "professions":
                        case "blocks":
                        case "items":
                            condition = choices(value, actual);
                            break;
                        case "baby":
                        case "creeperCharged":
                        case "playerCreated":
                        case "angry":
                        case "moving":
                            if (!value.equals("true") && !value.equals("false"))
                                throw new IllegalArgumentException("Invalid boolean rule: " + key);
                            condition = value::equals;
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported CEM rule property: " + key);
                    }
                    String field = actual.equals("minHeight") || actual.equals("maxHeight") ? "heights" : actual;
                    if (field.equals("blocks") || field.equals("items")) {
                        boolean invert = value.startsWith("!");
                        Predicate<String> item = choices(invert ? value.substring(1) : value, field);
                        rule.conditions.add(
                            f -> f.values(field)
                                .stream()
                                .anyMatch(item) != invert);
                    } else rule.conditions.add(f -> condition.test(f.value(field)));
                }
            }
            rules.add(rule);
        }
    }

    public Selection select(Facts facts) {
        for (Rule rule : rules) {
            boolean matches = true;
            for (Predicate<Facts> condition : rule.conditions) if (!condition.test(facts)) {
                matches = false;
                break;
            }
            if (matches) {
                long chosen = Math.floorMod(facts.seed(), rule.total);
                for (int i = 0; i < rule.models.length; i++) {
                    chosen -= rule.weights[i];
                    if (chosen < 0) return new Selection(rule.models[i], rule.index);
                }
            }
        }
        return new Selection(1, 0);
    }

    public boolean empty() {
        return rules.isEmpty();
    }

    public static Predicate<String> match(String expression) {
        boolean invert = expression.startsWith("!");
        String value = invert ? expression.substring(1) : expression;
        Predicate<String> positive;
        if (value.startsWith("raw:")) {
            String literal = value.substring(4);
            positive = literal::equals;
        } else if (value.startsWith("exists:")) {
            boolean exists = Boolean.parseBoolean(value.substring(7));
            positive = v -> (v != null) == exists;
        } else {
            boolean insensitive = value.startsWith("ipattern:") || value.startsWith("iregex:");
            boolean glob = value.startsWith("pattern:") || value.startsWith("ipattern:");
            boolean regex = value.startsWith("regex:") || value.startsWith("iregex:");
            if (glob || regex) {
                String body = value.substring(value.indexOf(':') + 1);
                if (glob) {
                    StringBuilder pattern = new StringBuilder();
                    for (char c : body.toCharArray())
                        pattern.append(c == '*' ? ".*" : c == '?' ? "." : Pattern.quote(String.valueOf(c)));
                    body = pattern.toString();
                }
                Pattern compiled = Pattern
                    .compile(body, insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
                positive = v -> v != null && compiled.matcher(v)
                    .matches();
            } else positive = value::equals;
        }
        return v -> positive.test(v) != invert;
    }

    public static Predicate<String> range(String expression) {
        List<double[]> ranges = new ArrayList<>();
        for (String token : expression.trim()
            .replaceAll("\\s+-\\s+", "-")
            .split("\\s+")) {
            Matcher match = RANGE.matcher(token);
            if (!match.matches()) throw new IllegalArgumentException("Invalid range: " + token);
            double min = Double.parseDouble(match.group(1)),
                max = match.group(2) == null ? min : Double.parseDouble(match.group(2));
            if (max < min) throw new IllegalArgumentException("Reversed range: " + token);
            ranges.add(new double[] { min, max });
        }
        return value -> {
            if (value == null) return false;
            double number = Double.parseDouble(value);
            for (double[] r : ranges) if (number >= r[0] && number <= r[1]) return true;
            return false;
        };
    }

    private static Predicate<String> choices(String expression, String type) {
        String positive = expression.startsWith("!") ? expression.substring(1) : expression;
        if (positive.matches("(?s)(?:i?regex|i?pattern):.*")) {
            Predicate<String> pattern = match(expression);
            return value -> pattern.test(value == null ? null : normalize(value, type));
        }
        boolean invert = expression.startsWith("!");
        String[] choices = (invert ? expression.substring(1) : expression).split("\\s+");
        return value -> {
            boolean found = false;
            if (value != null) for (String choice : choices) {
                String normalized = normalize(choice, type);
                String actual = normalize(value, type);
                if (normalized.equals(actual)) {
                    found = true;
                    break;
                }
                if (type.equals("blocks") && actual.startsWith(normalized + ":")) {
                    found = true;
                    break;
                }
                if (type.equals("blocks")) {
                    int split = normalized.lastIndexOf(':');
                    int actualSplit = actual.lastIndexOf(':');
                    if (split > 0 && actualSplit > 0
                        && normalized.substring(0, split)
                            .equals(actual.substring(0, actualSplit))) {
                        String metadata = normalized.substring(split + 1);
                        if (metadata.matches("[0-9,-]+")
                            && range(metadata.replace(',', ' ')).test(actual.substring(actualSplit + 1))) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            return found != invert;
        };
    }

    private static Predicate<String> stringChoices(String expression) {
        String positive = expression.startsWith("!") ? expression.substring(1) : expression;
        if (positive.matches("(?s)(?:i?regex|i?pattern):.*")) return match(expression);
        Set<String> choices = new HashSet<>();
        Matcher tokens = Pattern.compile("\"([^\"]*)\"|([^\\s]+)")
            .matcher(positive);
        while (tokens.find()) choices.add(tokens.group(1) == null ? tokens.group(2) : tokens.group(1));
        return value -> choices.contains(value) != expression.startsWith("!");
    }

    public static String normalize(String value, String type) {
        String result = value.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "");
        if (type.equals("biomes")) {
            result = result.replace("_", "")
                .replace(" ", "");
            switch (result) {
                case "snowyplains":
                case "snowytundra":
                    return "iceplains";
                case "icespikes":
                    return "iceplainsspikes";
                case "windswepthills":
                case "mountains":
                    return "extremehills";
                case "windsweptforest":
                case "woodedmountains":
                    return "extremehills+";
                case "snowytaiga":
                    return "coldtaiga";
                case "snowytaigahills":
                    return "coldtaigahills";
                case "oldgrowthpinetaiga":
                case "gianttreetaiga":
                    return "megataiga";
                case "oldgrowthsprucetaiga":
                case "giantsprucetaiga":
                    return "megasprucetaiga";
                case "snowybeach":
                    return "coldbeach";
                case "stonyshore":
                case "stoneshore":
                    return "stonebeach";
                case "badlands":
                    return "mesa";
                case "woodedbadlands":
                    return "mesaplateauf";
                case "darkforest":
                    return "roofedforest";
                case "swamp":
                    return "swampland";
                case "nether":
                case "netherwastes":
                    return "hell";
                case "theend":
                case "end":
                    return "sky";
                default:
                    return result;
            }
        }
        if (type.equals("blocks")) {
            result = BLOCK_ALIASES.getOrDefault(result, result);
            if (result.startsWith("etfuturum:")) {
                String shortName = result.substring(10)
                    .split(":")[0];
                if (EFR_PLANTS.contains(shortName)) result = result.substring(10);
            }
        }
        return result;
    }

    private static int[] integers(String value) {
        List<Integer> values = new ArrayList<>();
        for (String token : value.trim()
            .split("\\s+")) {
            Matcher matcher = RANGE.matcher(token);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid indices: " + value);
            int min = Integer.parseInt(matcher.group(1)),
                max = matcher.group(2) == null ? min : Integer.parseInt(matcher.group(2));
            if (min < 0 || max < min || max - min > 4096 || values.size() + max - min + 1 > 4096)
                throw new IllegalArgumentException("Excessive/invalid CEM indices");
            for (int n = min; n <= max; n++) values.add(n);
        }
        return values.stream()
            .mapToInt(Integer::intValue)
            .toArray();
    }

    private static final class Rule {

        final int index;
        final int[] models;
        int[] weights;
        long total;
        final List<Predicate<Facts>> conditions = new ArrayList<>();

        Rule(int index, int[] models) {
            this.index = index;
            this.models = models;
            this.weights = new int[models.length];
            Arrays.fill(weights, 1);
            for (int model : models)
                if (model < 1) throw new IllegalArgumentException("CEM model index must be positive");
        }
    }
}
