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
        /** A model's evaluation reads these by the slots it fixed when compiling; null elsewhere. */
        public final CemVariables slots;
        public final double[] inputValues;
        public final double[] renderValues;

        public Context(double[] pose, Map<String, Double> variables, Map<String, Double> inputs) {
            this(pose, variables, inputs, new java.util.HashMap<>());
        }

        public Context(double[] pose, Map<String, Double> variables, Map<String, Double> inputs,
            Map<String, Double> renderProperties) {
            this(pose, variables, inputs, renderProperties, null, null, null);
        }

        public Context(double[] pose, CemVariables variables, Map<String, Double> inputs,
            Map<String, Double> renderProperties, double[] inputValues, double[] renderValues) {
            this(pose, variables, inputs, renderProperties, variables, inputValues, renderValues);
        }

        private Context(double[] pose, Map<String, Double> variables, Map<String, Double> inputs,
            Map<String, Double> renderProperties, CemVariables slots, double[] inputValues, double[] renderValues) {
            this.renderProperties = renderProperties;
            this.pose = pose;
            this.variables = variables;
            this.inputs = inputs;
            this.slots = slots;
            this.inputValues = inputValues;
            this.renderValues = renderValues;
        }
    }
}
