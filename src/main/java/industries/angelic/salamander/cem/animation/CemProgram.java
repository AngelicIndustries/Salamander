package industries.angelic.salamander.cem.animation;

/** A model's whole animation program, compiled to one class by {@link CemBytecode}. */
public abstract class CemProgram {

    /** Interpreted pieces the compiled code calls, such as {@code print()}. Public for the generated class. */
    public final CemExpression[] fallbacks;

    protected CemProgram(CemExpression[] fallbacks) {
        this.fallbacks = fallbacks;
    }

    /** Runs every assignment in order, as the interpreter does. */
    public abstract void run(CemExpression.Context context);
}
