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

import industries.angelic.salamander.cem.animation.CemNbtTest;

/** Ordered Random Entities predicates shared by model and texture selection. */
public final class CemRules {

    public interface Facts extends CemNbtTest.Source {

        String value(String key);

        default List<String> values(String key) {
            String value = value(key);
            return value == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(value);
        }

        long seed();

        /** The seed of the entity this one rides, or its own seed when it rides nothing, as ETF's vehicle seed. */
        default long vehicleSeed() {
            return seed();
        }
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
    /** ETF's other names for rule properties. */
    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        alias("biomes", "biome");
        alias("blocks", "block", "blockSpawned");
        alias("heights", "height");
        alias("sizes", "size");
        alias("name", "names");
        alias("teams", "team");
        alias("items", "item");
        alias("colors", "collarColors");
        alias("maxHealth", "max_health");
        alias("moving", "is_moving");
        alias("angry", "isAngry", "is_angry", "aggressive", "is_aggressive");
        alias("creeperCharged", "creeper_charged");
        alias("playerCreated", "player_created");
        alias("jumpStrength", "jump", "jumpHeight");
        alias("speed", "maxSpeed", "speeds");
        alias("distanceFromPlayer", "distance");
        alias("monthDay", "dayMonth");
        alias("weekDay", "dayWeek");
        alias("yearDay", "dayYear");
        alias("modLoaded", "modsLoaded");
        alias("resourcepack", "resourcepackLoaded");
        alias("creative", "isCreative");
        alias("clientPlayer", "isClientPlayer");
        alias("teammate", "isTeammate");
    }
    /** Keys that configure a rule rather than test the entity. */
    private static final Set<String> RULE_KEYS = new HashSet<>(
        Arrays.asList("models", "textures", "skins", "weights", "seedOffset", "seedSource"));
    private final List<Rule> rules = new ArrayList<>();
    /**
     * What was skipped while reading, for the caller to log with the file's name. As in ETF, an unknown or unreadable
     * condition is left out of its rule, and a rule whose choices cannot be read is left out of the file.
     */
    public final List<String> warnings = new ArrayList<>();

    private static void alias(String name, String... aliases) {
        for (String alias : aliases) ALIASES.put(alias, name);
    }

    public CemRules(Properties properties) {
        this(properties, false);
    }

    public CemRules(Properties properties, boolean textures) {
        SortedSet<Integer> indices = new TreeSet<>();
        String keys = textures ? "(?:textures|skins)" : "models";
        for (String key : properties.stringPropertyNames()) if (key.matches(keys + "\\.[1-9][0-9]{0,4}"))
            indices.add(Integer.parseInt(key.substring(key.indexOf('.') + 1)));
        for (int index : indices) {
            String chosen = textures
                ? properties.getProperty("textures." + index, properties.getProperty("skins." + index))
                : properties.getProperty("models." + index);
            int[] choices;
            try {
                choices = integers(chosen);
            } catch (IllegalArgumentException exception) {
                warnings.add("rule " + index + " skipped: " + exception.getMessage());
                continue;
            }
            Rule rule = new Rule(index, choices);
            weights(rule, properties.getProperty("weights." + index));
            seed(rule, properties.getProperty("seedOffset." + index), properties.getProperty("seedSource." + index));
            for (String key : properties.stringPropertyNames()) {
                String suffix = "." + index;
                String value = properties.getProperty(key)
                    .trim();
                try {
                    if (key.startsWith("nbt." + index + ".")) {
                        String path = key.substring(("nbt." + index + ".").length());
                        CemNbtTest test = CemNbtTest.of(value);
                        rule.conditions.add(f -> test.test(f, path));
                        continue;
                    }
                    if (!key.endsWith(suffix)) continue;
                    String type = key.substring(0, key.length() - suffix.length());
                    if (RULE_KEYS.contains(type)) continue;
                    Predicate<Facts> condition = condition(ALIASES.getOrDefault(type, type), value);
                    if (condition == null) warnings.add(key + " is not supported here; ignored");
                    else rule.conditions.add(condition);
                } catch (IllegalArgumentException exception) {
                    warnings.add(key + "=" + value + " ignored: " + exception.getMessage());
                }
            }
            rules.add(rule);
        }
    }

    /** Fewer weights than choices take their average, extra ones are dropped; unusable weights pick uniformly. */
    private void weights(Rule rule, String value) {
        if (value == null) return;
        int[] weights;
        try {
            weights = integers(value);
        } catch (IllegalArgumentException exception) {
            warnings.add("weights." + rule.index + " ignored: " + exception.getMessage());
            return;
        }
        if (weights.length != rule.models.length) {
            warnings.add(
                "weights." + rule.index + " has " + weights.length + " weights for " + rule.models.length + " choices");
            long sum = 0;
            for (int weight : weights) sum += weight;
            int given = weights.length;
            weights = Arrays.copyOf(weights, rule.models.length);
            if (given < weights.length)
                Arrays.fill(weights, given, weights.length, given == 0 ? 1 : (int) (sum / given));
        }
        long total = 0;
        for (int weight : weights) total += weight;
        if (total <= 0) {
            warnings.add("weights." + rule.index + " sum to zero; choosing uniformly");
            return;
        }
        rule.weights = weights;
        rule.total = total;
    }

    private void seed(Rule rule, String offset, String source) {
        if (offset != null) try {
            rule.offset = Integer.parseInt(offset.trim());
        } catch (NumberFormatException exception) {
            warnings.add("seedOffset." + rule.index + "=" + offset + " ignored");
        }
        if (source == null) return;
        if (source.trim()
            .equals("vehicle")) rule.vehicle = true;
        else if (!source.trim()
            .equals("entity")) warnings.add("seedSource." + rule.index + "=" + source + " ignored");
    }

    /** One rule condition, or null for a property this version cannot test. */
    private static Predicate<Facts> condition(String type, String value) {
        switch (type) {
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
            case "difficulty":
            case "clientGameMode":
            case "temperature": {
                Predicate<String> range = range(value);
                return f -> range.test(f.value(type));
            }
            case "minHeight": {
                double minimum = Double.parseDouble(value);
                return f -> {
                    String height = f.value("heights");
                    return height != null && Double.parseDouble(height) >= minimum;
                };
            }
            case "maxHeight": {
                double maximum = Double.parseDouble(value);
                return f -> {
                    String height = f.value("heights");
                    return height != null && Double.parseDouble(height) <= maximum;
                };
            }
            case "health": {
                Predicate<String> range = range(value.replace("%", ""));
                String field = value.contains("%") ? "healthPercent" : "health";
                return f -> range.test(f.value(field));
            }
            case "name": {
                Predicate<String> name = match(value);
                return f -> name.test(f.value(type));
            }
            case "teams":
            case "language":
            case "dimension": {
                Predicate<String> choices = stringChoices(
                    type.equals("dimension") ? value.replace("minecraft:", "") : value);
                return f -> choices.test(f.value(type));
            }
            case "biomes":
            case "colors":
            case "weather":
            case "professions": {
                Predicate<String> choices = choices(value, type);
                return f -> choices.test(f.value(type));
            }
            case "blocks":
            case "items":
            case "blockBelow":
            case "blockBelowSolid": {
                boolean invert = value.startsWith("!");
                Predicate<String> item = choices(
                    invert ? value.substring(1) : value,
                    type.equals("items") ? "items" : "blocks");
                return f -> f.values(type)
                    .stream()
                    .anyMatch(item) != invert;
            }
            case "modLoaded":
            case "resourcepack": {
                // As ETF: true when any loaded mod or pack matches.
                Predicate<String> name = stringChoices(value);
                return f -> f.values(type)
                    .stream()
                    .anyMatch(name);
            }
            case "baby":
            case "creeperCharged":
            case "playerCreated":
            case "angry":
            case "moving":
            case "hardcore":
            case "creative":
            case "clientPlayer":
            case "teammate":
            case "usingShaders":
                if (!value.equals("true") && !value.equals("false"))
                    throw new IllegalArgumentException("expected true or false");
                return f -> value.equals(f.value(type));
            default:
                return null;
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
                long seed = rule.vehicle ? facts.vehicleSeed() : facts.seed();
                // As ETF and OptiFine: the seed's offset applies OptiFine's integer hash.
                if (rule.offset != 0) seed = Math.abs((int) seed ^ hash(rule.offset));
                long chosen = Math.floorMod(seed, rule.total);
                for (int i = 0; i < rule.models.length; i++) {
                    chosen -= rule.weights[i];
                    if (chosen < 0) return new Selection(rule.models[i], rule.index);
                }
            }
        }
        return new Selection(1, 0);
    }

    private static int hash(int x) {
        x ^= 0x3D ^ x >> 16;
        x += x << 3;
        x ^= x >> 4;
        x *= 668265261;
        x ^= x >> 15;
        return x;
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
        int offset;
        boolean vehicle;
        final List<Predicate<Facts>> conditions = new ArrayList<>();

        Rule(int index, int[] models) {
            this.index = index;
            this.models = models;
            this.weights = new int[models.length];
            Arrays.fill(weights, 1);
            total = models.length;
            // As ETF, a choice below 1 makes the whole file invalid.
            for (int model : models)
                if (model < 1) throw new IllegalArgumentException("CEM model index must be positive");
        }
    }
}
