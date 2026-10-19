package industries.angelic.salamander.cem.client;

import java.util.Map;

public interface CemEntityInputs {

    void salamander$cemInputs(Map<String, Double> inputs, float partialTicks);

    default float salamander$cemPreviousBodyYaw(float original) {
        return original;
    }
}
