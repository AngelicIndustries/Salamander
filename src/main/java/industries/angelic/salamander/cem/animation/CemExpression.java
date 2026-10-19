package industries.angelic.salamander.cem.animation;

import java.util.Map;

/** A compiled CEM expression. Angles in mathematical functions are radians. */
@FunctionalInterface
public interface CemExpression {

    double evaluate(Context context);

    final class Context {

        public final double[] pose;
        public final Map<String, Double> variables;
        public final Map<String, Double> inputs;
        public final Map<String, Double> renderProperties;

        public Context(double[] pose, Map<String, Double> variables, Map<String, Double> inputs) {
            this(pose, variables, inputs, new java.util.HashMap<>());
        }

        public Context(double[] pose, Map<String, Double> variables, Map<String, Double> inputs,
            Map<String, Double> renderProperties) {
            this.renderProperties = renderProperties;
            this.pose = pose;
            this.variables = variables;
            this.inputs = inputs;
        }
    }
}
