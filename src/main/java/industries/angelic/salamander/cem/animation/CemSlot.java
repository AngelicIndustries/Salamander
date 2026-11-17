package industries.angelic.salamander.cem.animation;

/**
 * A model's own value, resolved to an array slot of {@link CemExpression.Context} when the model compiled. The
 * bytecode compiler reads the same slot directly.
 */
public abstract class CemSlot implements CemExpression {

    /** A part property: {@code pose[index]}. */
    public static final int POSE = 0;
    /** {@code var.*} or {@code varb.*}: the entity's {@link CemVariables} slot. */
    public static final int VARIABLE = 1;
    /** An animation input, looked up once per evaluation: {@code inputValues[index]}. */
    public static final int INPUT = 2;
    /** A {@code render.*} property: {@code renderValues[index]}. */
    public static final int RENDER = 3;

    public final int kind;
    public final int index;

    private CemSlot(int kind, int index) {
        this.kind = kind;
        this.index = index;
    }

    public static CemSlot of(int kind, int index) {
        switch (kind) {
            case POSE:
                return new CemSlot(kind, index) {

                    @Override
                    public double evaluate(Context context) {
                        return context.pose[this.index];
                    }
                };
            case VARIABLE:
                return new CemSlot(kind, index) {

                    @Override
                    public double evaluate(Context context) {
                        return context.slots.get(this.index);
                    }
                };
            case INPUT:
                return new CemSlot(kind, index) {

                    @Override
                    public double evaluate(Context context) {
                        return context.inputValues[this.index];
                    }
                };
            case RENDER:
                return new CemSlot(kind, index) {

                    @Override
                    public double evaluate(Context context) {
                        return context.renderValues[this.index];
                    }
                };
            default:
                throw new IllegalArgumentException("Unknown CEM slot kind " + kind);
        }
    }
}
